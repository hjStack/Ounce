
package ounce.market.demo.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.cart.entity.Cart;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.cart.repository.CartRepository;
import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.order.dto.request.OrderCreateRequest;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.order.entity.OrderItem;
import ounce.market.demo.order.entity.OrderStatus;
import ounce.market.demo.order.repository.OrderRepository;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.StockRedisRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartProductRepository cartProductRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StockRedisRepository stockRedisRepository;

    @Transactional
    public Long createOrderFromCart(Long memberId, OrderCreateRequest request) {

        // 1. 회원 · 장바구니 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Cart cart = cartRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        // 2. 선택 상품 조회
        List<CartProduct> selectedCartProducts =
                cartProductRepository.findAllById(request.getSelectedCartProductIds());
        if (selectedCartProducts.isEmpty()) {
            throw new IllegalArgumentException("선택된 상품이 없어 결제를 진행할 수 없습니다.");
        }

        // 3. 보안 검증
        for (CartProduct cp : selectedCartProducts) {
            if (!cp.getCart().getCartId().equals(cart.getCartId())) {
                throw new IllegalArgumentException("본인의 장바구니에 담긴 상품만 결제할 수 있습니다.");
            }
        }

        // 4. 🔑 Redis 재고 차감 (선착순 관문). 실패 시 여태 깎은 것 보상 후 종료
        List<CartProduct> decreased = new ArrayList<>();
        try {
            for (CartProduct cp : selectedCartProducts) {
                stockRedisRepository.decrease(cp.getProduct().getProductId(), cp.getQuantity());
                decreased.add(cp);   // 성공한 것만 기록
            }
        } catch (RuntimeException e) {
            // 이번 주문에서 이미 깎은 재고 되돌리기
            rollbackRedisStock(decreased);
            throw e;
        }

        // 5. 여기부터 DB 작업. 실패하면 Redis 재고 전부 보상해야 함
        try {
            long totalAmount = 0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartProduct cp : selectedCartProducts) {
                Product product = cp.getProduct();
                int quantity = cp.getQuantity();

                product.decreaseStock(quantity);           // DB 재고도 차감 (최종 장부)
                long unitPrice = product.getSalePrice();
                totalAmount += unitPrice * quantity;

                orderItems.add(OrderItem.builder()
                        .product(product)
                        .price(Math.toIntExact(unitPrice))
                        .quantity(quantity)
                        .build());
            }

            member.deductPoint(Math.toIntExact(totalAmount));   // 포인트 (부족 시 예외)

            Order order = Order.builder()
                    .member(member)
                    .totalAmount(Math.toIntExact(totalAmount))
                    .status(OrderStatus.PAYMENT_COMPLETED)
                    .build();
            orderItems.forEach(order::addOrderItem);

            Delivery delivery = Delivery.builder()
                    .deliveryType(request.getDeliveryType())
                    .build();
            order.AssignDelivery(delivery);

            orderRepository.save(order);
            cartProductRepository.deleteAll(selectedCartProducts);

            return order.getOrderId();

        } catch (RuntimeException e) {
            // DB 처리 실패 → Redis 재고 되돌리기 (DB는 @Transactional이 알아서 롤백)
            rollbackRedisStock(selectedCartProducts);
            throw e;
        }
    }

    private void rollbackRedisStock(List<CartProduct> products) {
        for (CartProduct cp : products) {
            try {
                stockRedisRepository.increase(cp.getProduct().getProductId(), cp.getQuantity());
            } catch (RuntimeException ex) {
                // 보상 실패는 로그로 남겨 반드시 추적 (여기서 또 던지면 원래 예외를 덮음)
                log.error("Redis 재고 보상 실패 productId={}, qty={}",
                        cp.getProduct().getProductId(), cp.getQuantity(), ex);
            }
        }
    }
}
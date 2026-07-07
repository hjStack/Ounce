
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
import ounce.market.demo.order.dto.response.OrderResponse;
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

        /**
        사용자가 장바구니에서 상품 몇 개를 체크하고 "주문하기"를 누르면, 그 요청이 OrderController로 들어와요.
        컨트롤러는 JWT 토큰에서 로그인한 회원 id를 꺼내고(CustomUserDetails),
        어떤 장바구니 상품을 골랐는지(selectedCartProductIds)와
        배송 종류(deliveryType)를 담은 요청을 OrderService.createOrderFromCart로 넘깁니다.
        여기서부터 하나의 트랜잭션 시작
         */

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
        /** Redis 재고
         -> 선택한 상품마다 Redis에서 재고 원자적 차감 By Lua Script
         -> 재고가 충분하면 통과, 부족하면 재고 부족
         -> 수천 명이 동시에 몰려도 대부분을 Redis 단계에서 마이크로초 단위로 걸러내고 실제 재고가 되는 사람만 다음으로 보낸다는 거예요.
         만약 여러 상품 중 하나라도 재고가 모자라서 실패하면, 그때까지 이 주문에서 깎은 다른 상품 재고를 도로 되돌리고(보상) 종료합니다.

         레디스를 통과하면 DB 처리 -> 상품마다 DB 재고 차감 -> 판매가로 총액을 계산해서 주문 시점 가격을 OrderItem에 스냅샷으로 박아둠

         */
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

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long memberId) {
        return orderRepository.findAllByMemberMemberIdOrderByOrderIdDesc(memberId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
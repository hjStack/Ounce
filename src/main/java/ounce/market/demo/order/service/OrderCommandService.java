package ounce.market.demo.order.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.order.entity.OrderItem;
import ounce.market.demo.order.entity.OrderStatus;
import ounce.market.demo.order.repository.OrderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final CartProductRepository cartProductRepository;

    // 🚨 핵심: 오직 데이터를 수정하는 이 부분만 트랜잭션으로 꽉 묶어줍니다!
    @Transactional
    public Long executeOrderTransaction(Member member, List<CartProduct> products, int totalAmount) {

        // 1. 포인트 차감 (Update)
        member.deductPoint(totalAmount);

        // 2. 주문(Order) 통 생성 (Insert)
        Order order = Order.builder()
                .member(member)
                .totalAmount(totalAmount)
                .status(OrderStatus.PAYMENT_COMPLETED)
                .build();

        // 3. 선택된 장바구니 상품들을 주문 상품(OrderItem)으로 변환
        for (CartProduct cp : products) {
            OrderItem orderItem = OrderItem.builder()
                    .product(cp.getProduct())
                    .price(Math.toIntExact(cp.getProduct().getBasePrice()))
                    .quantity(cp.getQuantity())
                    .build();

            order.addOrderItem(orderItem);
        }

        // 4. 주문 및 주문 상품 DB에 저장
        orderRepository.save(order);

        // 5. 결제가 완료된 상품들 장바구니에서 삭제 (Delete)
        cartProductRepository.deleteAll(products);

        return order.getOrderId();
    }
}
package ounce.market.demo.order.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.coupon.entity.Coupon;
import ounce.market.demo.coupon.repository.CouponRepository;
import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.delivery.repository.DeliveryRepository;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.order.dto.request.OrderCreateRequest;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.order.entity.OrderItem;
import ounce.market.demo.order.entity.OrderStatus;
import ounce.market.demo.order.repository.OrderRepository;
import ounce.market.demo.point.entity.PointHistory;
import ounce.market.demo.point.entity.PointType;
import ounce.market.demo.point.repository.PointHistoryRepository;
import ounce.market.demo.product.repository.StockRedisRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final CartProductRepository cartProductRepository;
    private final StockRedisRepository stockRedisRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final DeliveryRepository deliveryRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 🚨 핵심: 오직 데이터를 수정하는 이 부분만 트랜잭션으로 꽉 묶어줍니다!
    @Transactional
    public Long executeOrderTransaction(Long memberId, List<CartProduct> products, int totalAmount,
                                        int paymentAmount, OrderCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 1. 포인트 차감 (Update)
        member.deductPoint(paymentAmount);

        // 2. 주문(Order) 통 생성 (Insert)
        Order order = Order.builder()
                .member(member)
                .totalAmount(paymentAmount)
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

        if (request.getDeliveryType() != null) {
            Delivery delivery = Delivery.builder()
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .zipCode(request.getZipCode())
                    .address(request.getAddress())
                    .addressDetail(request.getAddressDetail())
                    .deliveryType(request.getDeliveryType())
                    .build();
            order.AssignDelivery(delivery);
            deliveryRepository.save(delivery);
        }

        // todo 쿠폰 아이디가 널일경우
//        if (request.getCouponId() != null) {
//            Coupon coupon = couponRepository.findByCouponIdAndMemberMemberId(request.getCouponId(), memberId)
//                    .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
//            coupon.use(order, totalAmount);
//        }

        if (paymentAmount > 0) {
            pointHistoryRepository.save(PointHistory.builder()
                    .member(member)
                    .order(order)
                    .amount(-paymentAmount)
                    .type(PointType.USE)
                    .description("주문 결제")
                    .balanceAfter(member.getPoint())
                    .build());
        }

        // 5. 결제가 완료된 상품들 장바구니에서 삭제 (Delete)
        cartProductRepository.deleteAll(products);

        return order.getOrderId();
    }

    void rollbackRedisStock(List<CartProduct> products) {
        for (CartProduct cp : products) {
            try {
                stockRedisRepository.increase(cp.getProduct().getProductId(), cp.getQuantity());
            } catch (RuntimeException ex) {
                // 보상 실패는 로그로 남겨 반드시 (여기서 또 던지면 원래 예외를 덮음)
                log.error("Redis 재고 보상 실패 productId={}, qty={}",
                        cp.getProduct().getProductId(), cp.getQuantity(), ex);
            }
        }
    }
}

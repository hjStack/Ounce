package ounce.market.demo.order.service;

import lombok.RequiredArgsConstructor;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.order.entity.OrderItem;
import ounce.market.demo.order.entity.OrderStatus;
import ounce.market.demo.order.repository.OrderItemRepository;
import ounce.market.demo.order.repository.OrderRepository;
import ounce.market.demo.payment.entity.Payment;
import ounce.market.demo.payment.entity.PaymentStatus;
import ounce.market.demo.payment.repository.PaymentRepository;
import ounce.market.demo.point.entity.PointHistory;
import ounce.market.demo.point.entity.PointType;
import ounce.market.demo.point.repository.PointHistoryRepository;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.entity.Stock;
import ounce.market.demo.product.exception.ProductNotPurchasableException;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.StockRepository;
import ounce.market.demo.timeDeal.entity.DealStatus;
import ounce.market.demo.timeDeal.entity.TimeDeal;
import ounce.market.demo.timeDeal.repository.TimeDealRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 주문 1건을 처리하는 핵심 트랜잭션. 단독 호출 시 동시성 충돌(OptimisticLockingFailureException)이 그대로 터질 수 있으므로,
// 실제 구매 진입점은 재시도를 책임지는 OrderFacade를 통해야 한다.
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final TimeDealRepository timeDealRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Order placeOrder(Long memberId, Long productId, int quantity) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. memberId=" + memberId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + productId));
        Stock stock = stockRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다. productId=" + productId));

        TimeDeal activeDeal = null;
        int unitPrice;

        switch (product.getStatus()) {
            case ON_SALE -> unitPrice = product.getBasePrice();
            case TIME_DEAL -> {
                activeDeal = timeDealRepository.findByProduct_ProductIdAndStatus(productId, DealStatus.IN_PROGRESS)
                        .orElseThrow(() -> new IllegalStateException("진행중인 타임딜이 없습니다. productId=" + productId));
                if (!activeDeal.isInProgress(LocalDateTime.now())) {
                    throw new IllegalStateException("타임딜 시간이 종료되었습니다. productId=" + productId);
                }
                if (quantity > activeDeal.getMaxPurchaseLimit()) {
                    throw new IllegalArgumentException(
                            "1인당 구매 한도를 초과했습니다. limit=" + activeDeal.getMaxPurchaseLimit());
                }
                unitPrice = activeDeal.discountedPrice(product.getBasePrice());
            }
            default -> throw new ProductNotPurchasableException(productId, product.getStatus());
        }

        int totalPrice = unitPrice * quantity;

        // @Version 충돌 시 여기서 ObjectOptimisticLockingFailureException 발생 -> 호출자(OrderFacade)가 재시도
        stock.decrease(quantity);
        if (stock.getQuantity() == 0) {
            product.changeStatus(ProductStatus.SOLD_OUT);
            if (activeDeal != null) {
                activeDeal.markSoldOut();
            }
        }

        member.usePoint(totalPrice);

        Order order = Order.builder()
                .totalAmount(totalPrice)
                .member(member)
                .status(OrderStatus.PAYMENT_COMPLETED)
                .build();
        orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .price(unitPrice)
                .order(order)
                .product(product)
                .quantity(quantity)
                .build();
        orderItemRepository.save(orderItem);

        PointHistory pointHistory = PointHistory.builder()
                .member(member)
                .amount(-totalPrice)
                .type(PointType.USE)
                .build();
        pointHistoryRepository.save(pointHistory);

        Payment payment = Payment.builder()
                .order(order)
                .amount(totalPrice)
                .paymentMethod("POINT")
                .status(PaymentStatus.COMPLETED)
                .build();
        paymentRepository.save(payment);

        return order;
    }
}

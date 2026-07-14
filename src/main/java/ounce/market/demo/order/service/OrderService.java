
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
import ounce.market.demo.order.repository.OrderRepository;
import ounce.market.demo.product.repository.StockRedisRepository;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartProductRepository cartProductRepository;
    private final MemberRepository memberRepository;
    private final StockRedisRepository stockRedisRepository;
    private final OrderCommandService orderCommandService;

    /*
    "주문/결제 로직에서 DB 커넥션 점유 시간을 최소화하기 위해 조회(Read)와 쓰기(Write) 로직을 분리하는 구조로 리팩토링함.
     이를 통해 트랜잭션 범위를 축소하여 대용량 트래픽 상황에서의 데드락(Deadlock) 및 병목 현상을 선제적으로 방어함."
     */

    public Long createOrderFromCart(Long memberId, OrderCreateRequest request) {

    // 1. 순수 조회 (DB 커넥션을 낭비하지 않고 빠르게 읽기만 함)
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

    List<CartProduct> selectedProducts = cartProductRepository.findAllById(request.getSelectedCartProductIds());

    if (selectedProducts.isEmpty()) {
        throw new IllegalArgumentException("선택된 상품이 없어 결제를 진행할 수 없습니다.");
    }

    // 2. 검증 및 금액 계산 (DB 통신 없이 순수 자바 메모리에서 0.001초 만에 수행)
    int totalAmount = selectedProducts.stream()
            .mapToInt(cp -> Math.toIntExact(cp.getProduct().getBasePrice() * cp.getQuantity()))
            .sum();

    if (member.getPoint() < totalAmount) {
        throw new IllegalArgumentException("포인트가 부족합니다. (현재 잔액: " + member.getPoint() + "원)");
    }

    // 3. 진짜 쓰기(Write) 작업은 분리된 서비스로 넘겨버리기!
    return orderCommandService.executeOrderTransaction(member, selectedProducts, totalAmount);
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
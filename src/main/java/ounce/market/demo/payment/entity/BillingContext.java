package ounce.market.demo.payment.entity;

/**
 * 결제 한 번을 처리하는 동안 트랜잭션 사이를 건너가는 값.
 * <p>
 * 엔티티가 아니다. 이 값이 담고 있는 건 이미 SubscriptionCycle과 PaymentAttempt에
 * 커밋되어 있고, 여기 있는 사본은 PG 호출 구간에서 영속성 컨텍스트 없이 쓰기 위한 것이다.
 * 트랜잭션이 닫힌 뒤 엔티티를 들고 다니면 지연 로딩이 터지거나 detach된 상태를 만지게 된다.
 */
public record BillingContext(
        Long subscriptionId,
        Long memberId,
        Long cycleId,
        Long attemptId,
        int cycleNumber,
        long attemptNo,
        long amount,
        String idempotencyKey
) {

    public String orderName() {
        return "오운스 밀키트 구독 %d회차".formatted(cycleNumber);
    }
}
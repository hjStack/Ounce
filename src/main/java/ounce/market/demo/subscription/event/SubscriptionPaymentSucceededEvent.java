package ounce.market.demo.subscription.event;

/**
 * 구독 결제가 성공했다. 결제 트랜잭션이 커밋된 뒤에 발행된다.
 * <p>
 * 쿠폰 발급, 알림 발송처럼 "결제가 끝난 다음에 일어나면 되는 일"을 여기에 붙인다.
 * 결제 트랜잭션 안에서 직접 부르면 그쪽 실패가 결제를 롤백시킨다.
 */
public record SubscriptionPaymentSucceededEvent(
        Long memberId,
        Long subscriptionId,
        Long cycleId,
        int streakCount,
        boolean couponEligible
) {
}
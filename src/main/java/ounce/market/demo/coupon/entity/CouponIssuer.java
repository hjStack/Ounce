package ounce.market.demo.coupon.entity;

/**
 * 쿠폰 발급 포트. 4주 연속 결제 성공 시 호출된다.
 * 구현체는 같은 구독의 같은 연속 회차에 대해 중복 발급되지 않도록 막아야 한다
 * (구독ID + 연속회차 조합으로 유니크 제약을 거는 방식을 권장).
 */
public interface CouponIssuer {

    void issueSubscriptionStreakCoupon(Long memberId, Long subscriptionId, int streakCount, int amount);
}

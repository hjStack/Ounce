package ounce.market.demo.subscription.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import ounce.market.demo.coupon.entity.CouponIssuer;
import ounce.market.demo.subscription.entity.Subscription;

/**
 * 4주 연속 결제 성공 쿠폰 발급.
 * <p>
 * AFTER_COMMIT이라 결제가 확정된 뒤에만 돈다. 여기서 무슨 일이 나든 결제는 이미 끝났다.
 * REQUIRES_NEW를 붙인 이유는 AFTER_COMMIT 리스너가 기존 트랜잭션 밖에서 실행되기 때문이다.
 * 없으면 쓰기가 조용히 무시된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionCouponListener {

    private final CouponIssuer couponIssuer;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(SubscriptionPaymentSucceededEvent event) {
        if (!event.couponEligible()) {
            return;
        }
        try {
            couponIssuer.issueSubscriptionStreakCoupon(
                    event.memberId(), event.subscriptionId(),
                    event.streakCount(), Subscription.COUPON_AMOUNT);
        } catch (Exception e) {
            // 쿠폰이 안 나가도 결제는 유효하다. 수동 발급이 가능하도록 로그만 남긴다.
            log.error("연속 결제 쿠폰 발급 실패 memberId={} subscriptionId={} streak={}",
                    event.memberId(), event.subscriptionId(), event.streakCount(), e);
        }
    }
}
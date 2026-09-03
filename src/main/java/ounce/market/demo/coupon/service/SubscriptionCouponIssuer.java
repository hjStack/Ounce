package ounce.market.demo.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.coupon.entity.CouponIssuer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCouponIssuer implements CouponIssuer {

    // private final CouponRepository couponRepository;
    // private final MemberRepository memberRepository;

    /**
     * 4주 연속 결제 성공 쿠폰 발급.
     * <p>
     * 결제가 커밋된 뒤 SubscriptionCouponListener가 호출한다.
     * 예외를 삼키지 않는다 — 던지면 리스너가 잡아 로그를 남기고,
     * 결제는 이미 확정돼 있어 영향받지 않는다.
     * <p>
     * 중복 발급 방어는 이 안에서 해야 한다. (subscription_id, streak_count)에
     * 유니크 제약을 걸어두면 어떤 경로로 두 번 불려도 한 장만 나간다.
     */
    @Override
    @Transactional
    public void issueSubscriptionStreakCoupon(Long memberId, Long subscriptionId,
                                              int streakCount, int amount) {
        // 실제 발급 로직. Coupon 엔티티에 맞춰 채운다.
        // Member member = memberRepository.findById(memberId).orElseThrow();
        // couponRepository.save(Coupon.issueForSubscription(member, subscriptionId, streakCount, amount));

        log.info("구독 연속 결제 쿠폰 발급 memberId={} subscriptionId={} streak={} amount={}",
                memberId, subscriptionId, streakCount, amount);
    }
}
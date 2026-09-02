package ounce.market.demo.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.subscription.Exception.SubscriptionException;

// 위 두 import가 빠지면 validateMealsPerWeek / requireActive 에서 컴파일 에러가 난다.

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscription",
        indexes = {
                @Index(name = "idx_subscription_member", columnList = "member_id"),
                // 매일 도는 배치가 "오늘 결제할 구독"을 훑는 경로
                @Index(name = "idx_subscription_billing", columnList = "status, next_billing_date")
        }
)
public class Subscription extends BaseEntity {

    /** 주당 최소/최대 끼수. 3끼는 상자 고정비 비중이 커서 제외. */
    public static final int MIN_MEALS_PER_WEEK = 4;
    public static final int MAX_MEALS_PER_WEEK = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private int mealsPerWeek;

    @Column(nullable = false)
    private LocalDate startDate;

    /** 다음 결제 예정일. 해지되면 null. */
    private LocalDate nextBillingDate;

    /**
     * 연속 결제 성공 횟수. 쿠폰 발급 조건의 기준값이다.
     * 스킵은 유지, 결제 실패와 해지는 0으로 리셋한다.
     */
    @Column(nullable = false)
    private int consecutiveCount;

    /** 지금까지 만들어진 회차 번호. 1부터 시작한다. */
    @Column(nullable = false)
    private int lastCycleNumber;

    private LocalDate canceledAt;

    @Builder
    public Subscription(Member member, int mealsPerWeek, LocalDate startDate) {
        validateMealsPerWeek(mealsPerWeek);
        this.member = member;
        this.mealsPerWeek = mealsPerWeek;
        this.startDate = startDate;
        this.nextBillingDate = startDate;
        this.status = SubscriptionStatus.ACTIVE;
        this.consecutiveCount = 0;
        this.lastCycleNumber = 0;
    }

    private static void validateMealsPerWeek(int mealsPerWeek) {
        if (mealsPerWeek < MIN_MEALS_PER_WEEK || mealsPerWeek > MAX_MEALS_PER_WEEK) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_MEALS_PER_WEEK);
        }
    }

    // ===== 회차 생성 =====

    public int nextCycleNumber() {
        return ++this.lastCycleNumber;
    }

    // ===== 결제 결과 반영 =====

    /**
     * 결제 성공. 연속 횟수를 올리고 다음 결제일을 일주일 뒤로 민다.
     * 반환값이 갱신된 연속 횟수이며, 쿠폰 발급 판정은 이 값으로 한다.
     */
    public int onPaymentSucceeded() {
        requireActive();
        this.consecutiveCount++;
        this.nextBillingDate = this.nextBillingDate.plusWeeks(1);
        return this.consecutiveCount;
    }

    /**
     * 결제 실패. 연속 횟수가 끊긴다.
     * 구독 자체는 유지하고 상태만 PAYMENT_FAILED로 두어 재시도 여지를 남긴다.
     */
    public void onPaymentFailed() {
        this.consecutiveCount = 0;
        this.status = SubscriptionStatus.PAYMENT_FAILED;
    }

    /** 결제 수단 교체 후 재개 */
    public void resumeAfterPaymentFailure() {
        if (this.status != SubscriptionStatus.PAYMENT_FAILED) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = SubscriptionStatus.ACTIVE;
    }

    /**
     * 그 주만 건너뛴다. 여행이나 출장으로 한 주 쉬는 경우.
     * 연속 횟수는 유지한다 — 스킵으로 카운트가 리셋되면 사용자가 스킵을 못 쓴다.
     */
    public void skipNextCycle() {
        requireActive();
        this.nextBillingDate = this.nextBillingDate.plusWeeks(1);
    }

    public void changeMealsPerWeek(int mealsPerWeek) {
        requireActive();
        validateMealsPerWeek(mealsPerWeek);
        this.mealsPerWeek = mealsPerWeek;
    }

    /** 해지. 멱등. */
    public void cancel(LocalDate today) {
        if (this.status == SubscriptionStatus.CANCELED) {
            return;
        }
        this.status = SubscriptionStatus.CANCELED;
        this.nextBillingDate = null;
        this.consecutiveCount = 0;
        this.canceledAt = today;
    }

    public boolean isBillableOn(LocalDate today) {
        return this.status == SubscriptionStatus.ACTIVE
                && this.nextBillingDate != null
                && !this.nextBillingDate.isAfter(today);
    }

    public boolean isOwnedBy(Long memberId) {
        return member != null && member.getMemberId().equals(memberId);
    }

    private void requireActive() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
    }
}
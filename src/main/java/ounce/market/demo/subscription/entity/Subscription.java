package ounce.market.demo.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscription",
        indexes = {
                @Index(name = "idx_subscription_member", columnList = "member_id"),
                // 결제 배치: 오늘 결제할 ACTIVE 구독을 훑는 경로
                @Index(name = "idx_subscription_billing", columnList = "status, next_billing_date"),
                // 재시도 배치: 2시간마다 재시도 대상을 훑는 경로
                @Index(name = "idx_subscription_retry", columnList = "status, next_retry_at")
        }
)
public class Subscription extends BaseEntity {

    // ===== 정책 상수 =====

    /** 주당 끼수. 화면에서 4~7만 고를 수 있어 엔티티에서 검증하지 않는다. */
    public static final int MIN_MEALS_PER_WEEK = 4;
    public static final int MAX_MEALS_PER_WEEK = 7;

    /**
     * 회차 마감 시각(23:00). 결제일 23시 전에 확정된 회차는 다음날 새벽 배송,
     * 넘기면 그 다음날 새벽 배송이다. 결제 배치도 이 시각에 돈다.
     */
    public static final LocalTime ORDER_CUTOFF_TIME = LocalTime.of(23, 0);

    /** 연속 결제 성공 4회마다 쿠폰. */
    public static final int COUPON_STREAK = 4;
    public static final int COUPON_AMOUNT = 3_000;

    /** 한 회차에 허용되는 결제 시도 횟수(최초 1회 + 재시도 2회). */
    public static final int MAX_PAYMENT_ATTEMPTS = 3;
    // 2시간 간격으로 결제 시도
    public static final int RETRY_INTERVAL_HOURS = 2;

    /** 결제를 못 받고 넘어간 주가 이만큼 연속되면 해지. */
    // 3주동안 결제 안된 상태면 구독 해지
    public static final int MAX_FAILED_WEEK_STREAK = 3;

    // ===== 식별 / 연관 =====

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // ===== 상태 =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private int mealsPerWeek;

    /** 마감이 지난 뒤 끼수를 바꾸면 여기 담겼다가 다음 회차부터 적용된다. */
    private Integer pendingMealsPerWeek;

    @Column(nullable = false)
    private LocalDate startDate;

    /** 다음 결제 예정일. 해지되면 null. */
    private LocalDate nextBillingDate;

    /** 배송 1회 건너뛰기 전의 결제일. 취소 시 복원한다. */
    private LocalDate skippedBillingDate;

    /** 쉬어가기 종료일 = 결제를 다시 시작하는 날. PAUSED일 때만 값이 있다. */
    private LocalDate resumeDate;

    // ===== 카운터 =====

    /** 연속 결제 성공 횟수. 쿠폰 판정 기준. 실패와 해지에서 0으로 리셋한다. */
    @Column(nullable = false)
    private int consecutiveCount;

    /** 결제를 못 받고 넘어간 연속 주 수. 성공하면 0으로 리셋한다. */
    @Column(nullable = false)
    private int failedWeekStreak;

    /** 현재 회차에서 결제를 시도한 횟수. 회차가 끝나면 0으로 되돌린다. */
    @Column(nullable = false)
    private int paymentAttemptCount;

    private LocalDateTime nextRetryAt;

    /** 재시도 마감. 첫 실패한 날의 다음날 자정까지. */
    private LocalDateTime retryDeadline;

    /** 지금까지 만들어진 회차 번호. 1부터 시작한다. */
    @Column(nullable = false)
    private int lastCycleNumber;

    private LocalDate canceledAt;

    // ===== 생성 =====

    private Subscription(Member member, int mealsPerWeek, LocalDate startDate) {
        this.member = member;
        this.mealsPerWeek = mealsPerWeek;
        this.startDate = startDate;
        this.nextBillingDate = startDate;
        this.status = SubscriptionStatus.ACTIVE;
        this.consecutiveCount = 0;
        this.failedWeekStreak = 0;
        this.paymentAttemptCount = 0;
        this.lastCycleNumber = 0;
    }

    /**
     * 구독 시작. 가입일이 곧 첫 결제일이다.
     * <p>
     * 가입일과 결제일을 떼어놓으면 "구독은 만들어졌는데 결제는 안 된" 상태가 생기고,
     * 그 상태를 어떻게 정리할지 규칙을 따로 만들어야 한다. 같은 날로 묶으면 그 분기가 사라진다.
     * 9/3에 가입해 23시 전에 결제하면 9/4 새벽 배송, 다음 회차는 9/10이다.
     */
    public static Subscription start(Member member, int mealsPerWeek, LocalDate today) {
        return new Subscription(member, mealsPerWeek, today);
    }

    // ===== 배송일 계산 =====

    /**
     * 확정 시각 기준 새벽 배송일.
     * 23시를 넘기면 그 다음날, 아니면 다음날. (월 22시 → 화, 월 23시 30분 → 수)
     * <p>
     * 경계가 중요하다. 결제 배치가 23:00:00 정각에 돌기 때문에 정각을 "지났다"로 보면
     * 정상 결제된 회차가 전부 하루씩 밀린다. 정각은 마감 안쪽으로 친다.
     * 반대로 사용자의 변경 요청은 23:00:00이면 거절이다({@link #requireBeforeCutoff}).
     * 마감 시각에 배치와 사용자가 동시에 들어오면 배치가 이긴다.
     * <p>
     * 공휴일·권역 규칙이 붙기 시작하면 별도 DeliveryPolicy로 빼야 한다.
     */
    public static LocalDate deliveryDateOf(LocalDateTime confirmedAt) {
        return confirmedAt.toLocalTime().isAfter(ORDER_CUTOFF_TIME)
                ? confirmedAt.toLocalDate().plusDays(2)
                : confirmedAt.toLocalDate().plusDays(1);
    }

    /**
     * 다음 회차를 열어야 하는 구독인가.
     * <p>
     * 메뉴 창은 직전 회차 결제가 끝나면 곧바로 열리고 다음 결제일 23시에 닫힌다.
     * 결제일이 금요일이면 사용자는 그 주 내내 고르고 금요일 23시가 마감이다.
     * 창을 며칠 전에 여는 게 아니라 계속 열어두는 구조라 별도 오픈일 개념이 없다.
     */
    public boolean canOpenNextCycle() {
        return status == SubscriptionStatus.ACTIVE && nextBillingDate != null;
    }

    /** 이번 회차의 변경 마감 시각. 이 시각을 넘기면 쉬어가기·끼수 변경이 다음 회차로 밀린다. */
    public LocalDateTime currentCutoff() {
        if (nextBillingDate == null) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
        return nextBillingDate.atTime(ORDER_CUTOFF_TIME);
    }

    // ===== 회차 =====

    public int nextCycleNumber() {
        return ++this.lastCycleNumber;
    }

    /** 결제 배치가 오늘 집어가야 할 구독인지. */
    public boolean isBillableOn(LocalDate today) {
        return status == SubscriptionStatus.ACTIVE
                && nextBillingDate != null
                && !nextBillingDate.isAfter(today);
    }

    /** 재시도 배치가 지금 집어가야 할 구독인지. */
    public boolean isRetryDue(LocalDateTime now) {
        return status == SubscriptionStatus.PAYMENT_RETRYING
                && nextRetryAt != null
                && !now.isBefore(nextRetryAt)
                && !now.isAfter(retryDeadline);
    }

    // ===== 결제 결과 =====

    /**
     * 결제 성공. 실패 관련 상태를 전부 털어내고 다음 주로 넘긴다.
     * 반환값이 갱신된 연속 성공 횟수이며, 쿠폰 판정은 {@link #isCouponIssuable()}로 한다.
     */
    public int onPaymentSucceeded() {
        requireChargeable();
        this.consecutiveCount++;
        this.failedWeekStreak = 0;
        clearRetryState();
        this.status = SubscriptionStatus.ACTIVE;
        this.nextBillingDate = this.nextBillingDate.plusWeeks(1);
        applyPendingMeals();
        return this.consecutiveCount;
    }

    /** 4주 연속 성공마다 3천원 쿠폰. */
    public boolean isCouponIssuable() {
        return consecutiveCount > 0 && consecutiveCount % COUPON_STREAK == 0;
    }

    /**
     * 결제 실패. 시도 횟수와 마감이 남아 있으면 2시간 뒤로 재시도를 잡고,
     * 소진되면 그 주는 쉬어간다. 그렇게 넘긴 주가 연속 3주면 해지한다.
     */
    public PaymentFailureResult onPaymentFailed(LocalDateTime attemptedAt) {
        requireChargeable();
        if (paymentAttemptCount == 0) {
            // "다음날까지" — 첫 실패한 날의 다음날 끝까지만 재시도한다
            this.retryDeadline = attemptedAt.toLocalDate().plusDays(1).atTime(LocalTime.MAX);
        }
        this.paymentAttemptCount++;
        this.consecutiveCount = 0;

        LocalDateTime candidate = attemptedAt.plusHours(RETRY_INTERVAL_HOURS);
        if (paymentAttemptCount < MAX_PAYMENT_ATTEMPTS && !candidate.isAfter(retryDeadline)) {
            this.status = SubscriptionStatus.PAYMENT_RETRYING;
            this.nextRetryAt = candidate;
            return PaymentFailureResult.RETRY_SCHEDULED;
        }
        return giveUpThisCycle(attemptedAt.toLocalDate());
    }

    /**
     * 재시도해도 결과가 같은 실패(카드 정지, 유효기간 만료 등).
     * 2시간 뒤에 다시 긁어봐야 똑같이 거절당하므로 남은 시도를 쓰지 않고 이번 주를 넘긴다.
     */
    public PaymentFailureResult abandonCycle(LocalDateTime now, String reason) {
        requireChargeable();
        this.consecutiveCount = 0;
        return giveUpThisCycle(now.toLocalDate());
    }

    /** 재시도 마감이 지났는데 아직 PAYMENT_RETRYING으로 남아 있는 구독 정리용. */
    public PaymentFailureResult expireRetry(LocalDateTime now) {
        if (status != SubscriptionStatus.PAYMENT_RETRYING || now.isBefore(retryDeadline)) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        return giveUpThisCycle(now.toLocalDate());
    }

    /** 결제 수단을 바꿨을 때 다음 슬롯을 기다리지 않고 즉시 재시도. */
    public void retryNow(LocalDateTime now) {
        if (status != SubscriptionStatus.PAYMENT_RETRYING) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (now.isAfter(retryDeadline) || paymentAttemptCount >= MAX_PAYMENT_ATTEMPTS) {
            throw new SubscriptionException(SubscriptionErrorCode.RETRY_DEADLINE_PASSED);
        }
        this.nextRetryAt = now;
    }

    private PaymentFailureResult giveUpThisCycle(LocalDate today) {
        clearRetryState();
        this.failedWeekStreak++;
        if (failedWeekStreak >= MAX_FAILED_WEEK_STREAK) {
            cancel(today);
            return PaymentFailureResult.SUBSCRIPTION_CANCELED;
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.nextBillingDate = this.nextBillingDate.plusWeeks(1);
        applyPendingMeals();
        return PaymentFailureResult.WEEK_SKIPPED;
    }

    private void clearRetryState() {
        this.paymentAttemptCount = 0;
        this.nextRetryAt = null;
        this.retryDeadline = null;
    }

    // ===== 쉬어가기 =====

    /**
     * 이번 주만 쉬어간다. 결제일 23시 전까지만 가능하다.
     * 연속 성공 횟수는 유지한다 — 스킵으로 리셋되면 아무도 스킵을 안 쓴다.
     */
    // 이번주만 쉬어가기
    public void skipThisCycle(LocalDateTime now) {
        requireActive();
        requireBeforeCutoff(now);
        if (skippedBillingDate != null) {
            throw new SubscriptionException(SubscriptionErrorCode.SKIP_ALREADY_SET);
        }
        this.skippedBillingDate = nextBillingDate;
        this.nextBillingDate = this.nextBillingDate.plusWeeks(1);
        applyPendingMeals();
    }

    public void cancelSkip(LocalDateTime now) {
        requireActive();
        requireBeforeCutoff(now);
        if (skippedBillingDate == null) {
            throw new SubscriptionException(SubscriptionErrorCode.SKIP_NOT_SET);
        }
        this.nextBillingDate = skippedBillingDate;
        this.skippedBillingDate = null;
    }

    /**
     * 지정한 날짜까지 쉬어간다. resumeDate 당일이 다시 결제되는 날이다.
     * 그 사이 결제일은 전부 건너뛴다.
     */
    public void pauseUntil(LocalDateTime now, LocalDate resumeDate) {
        requireActive();
        requireBeforeCutoff(now);
        if (resumeDate == null || !resumeDate.isAfter(nextBillingDate)) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_RESUME_DATE);
        }
        this.status = SubscriptionStatus.PAUSED;
        this.resumeDate = resumeDate;
        this.nextBillingDate = resumeDate;
        this.skippedBillingDate = null;
    }

    /** 쉬어가기 해제. 배치가 resumeDate 도달분을, 사용자가 직접 누르면 즉시 재개한다. */
    public void resume(LocalDate today) {
        if (status != SubscriptionStatus.PAUSED) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.resumeDate = null;
        // 오늘 결제된 회차를 다시 청구하지 않고 다음 주부터 결제를 재개한다.
        this.nextBillingDate = today.plusWeeks(1);
        applyPendingMeals();
    }

    public boolean isResumeDue(LocalDate today) {
        return status == SubscriptionStatus.PAUSED
                && resumeDate != null
                && !resumeDate.isAfter(today);
    }

    // ===== 변경 / 해지 =====

    /**
     * 끼수 변경. 마감 전이면 이번 회차부터, 지났으면 다음 회차부터 적용된다.
     * 4~7 범위는 화면에서 막는다는 전제다.
     */
    public void changeMealsPerWeek(LocalDateTime now, int mealsPerWeek, SubscriptionCycle draftCycle) {
        if (status.isTerminal()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
        if (status == SubscriptionStatus.ACTIVE && now.isBefore(currentCutoff())) {
            this.mealsPerWeek = mealsPerWeek;
            this.pendingMealsPerWeek = null;

            draftCycle.changeMealsPerWeek(mealsPerWeek);
        } else {
            this.pendingMealsPerWeek = mealsPerWeek;
        }
    }

    private void applyPendingMeals() {
        if (pendingMealsPerWeek != null) {
            this.mealsPerWeek = pendingMealsPerWeek;
            this.pendingMealsPerWeek = null;
        }
    }

    /** 해지. 멱등. */
    public void cancel(LocalDate today) {
        if (status == SubscriptionStatus.CANCELED) {
            return;
        }
        this.status = SubscriptionStatus.CANCELED;
        this.nextBillingDate = null;
        this.resumeDate = null;
        this.consecutiveCount = 0;
        clearRetryState();
        this.canceledAt = today;
    }

    public boolean isOwnedBy(Long memberId) {
        return member != null && member.getMemberId().equals(memberId);
    }

    private void requireActive() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
    }

    /** 결제 결과를 반영받을 수 있는 상태인지. ACTIVE 첫 시도와 PAYMENT_RETRYING 재시도 둘 다 허용된다. */
    private void requireChargeable() {
        if (!status.isChargeable()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
    }

    private void requireBeforeCutoff(LocalDateTime now) {
        if (!now.isBefore(currentCutoff())) {
            throw new SubscriptionException(SubscriptionErrorCode.SKIP_DEADLINE_PASSED);
        }
    }
}

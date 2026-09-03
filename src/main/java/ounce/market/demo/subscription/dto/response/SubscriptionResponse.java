package ounce.market.demo.subscription.dto.response;

import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionCycle;
import ounce.market.demo.subscription.entity.SubscriptionCycleStatus;
import ounce.market.demo.subscription.entity.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 응답 DTO. 엔티티를 그대로 내보내지 않는다.
 * version, retryDeadline, paymentAttemptCount 같은 내부 상태는 화면이 알 필요가 없고,
 * 엔티티 필드를 하나 추가할 때마다 API 스펙이 조용히 바뀌는 것도 막아야 한다.
 */
public final class SubscriptionResponse {

    private SubscriptionResponse() {
    }

    public record Detail(
            Long subscriptionId,
            SubscriptionStatus status,
            int mealsPerWeek,
            /** 구독을 처음 시작한 날짜. 해지 후 재구독하면 새 구독의 시작일이다. */
            LocalDate startDate,
            /** 마감이 지난 뒤 바꾼 끼수. 다음 회차부터 적용된다. 없으면 null. */
            Integer pendingMealsPerWeek,
            LocalDate nextBillingDate,
            /** 다음 결제가 정상 진행됐을 때의 새벽 배송일. */
            LocalDate nextDeliveryDate,
            /** 이번 회차 변경 마감. 이 시각 전까지 쉬어가기와 메뉴 변경이 가능하다. */
            LocalDateTime changeDeadline,
            LocalDate resumeDate,
            boolean skipCancelable,
            int consecutiveCount,
            /** 쿠폰까지 남은 결제 횟수. 화면에 "3천원 쿠폰까지 2번!"으로 노출한다. */
            int remainingForCoupon,
            boolean changeable
    ) {

        public static Detail from(Subscription s, LocalDateTime now) {
            boolean active = s.getStatus() == SubscriptionStatus.ACTIVE && s.getNextBillingDate() != null;
            LocalDateTime deadline = active ? s.currentCutoff() : null;

            return new Detail(
                    s.getSubscriptionId(),
                    s.getStatus(),
                    s.getMealsPerWeek(),
                    s.getStartDate(),
                    s.getPendingMealsPerWeek(),
                    s.getNextBillingDate(),
                    active ? Subscription.deliveryDateOf(deadline) : null,
                    deadline,
                    s.getResumeDate(),
                    s.getSkippedBillingDate() != null,
                    s.getConsecutiveCount(),
                    Subscription.COUPON_STREAK - (s.getConsecutiveCount() % Subscription.COUPON_STREAK),
                    active && now.isBefore(deadline)
            );
        }
    }

    public record Cycle(
            int cycleNumber,
            SubscriptionCycleStatus status,
            int mealsPerWeek,
            long amount,
            LocalDate deliveryDate
    ) {

        public static Cycle from(SubscriptionCycle c) {
            return new Cycle(
                    c.getCycleNumber(),
                    c.getStatus(),
                    c.getMealsPerWeek(),
                    c.getAmount(),
                    c.getDeliveryDate()
            );
        }
    }

    /**
     * 결제를 동반한 요청의 응답. 가입(201)과 재결제(200)가 같은 모양을 쓴다.
     * <p>
     * 두 record로 나누면 필드가 똑같은데 이름만 다른 것이 생기고, 한쪽에 필드를 추가할 때
     * 다른 쪽을 빠뜨린다. 구독 ID는 subscription.subscriptionId에 들어 있고,
     * 가입 응답에는 Location 헤더에도 실린다.
     *
     * @param outcome PAID / FAILED / NOT_BILLABLE / NO_RESPONSE. 화면은 이 값으로 분기한다.
     * @param message 사용자에게 그대로 보여줄 문구
     */
    public record Checkout(
            String outcome,
            String message,
            Detail subscription
    ) {
    }
}

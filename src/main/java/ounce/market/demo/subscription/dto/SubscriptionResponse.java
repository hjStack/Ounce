package ounce.market.demo.subscription.dto;

import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionStatus;

import java.time.LocalDate;

public record SubscriptionResponse(
        Long subscriptionId,
        SubscriptionStatus status,
        int mealsPerWeek,
        LocalDate startDate,
        LocalDate nextBillingDate,
        int consecutiveCount,
        /** 다음 쿠폰까지 남은 결제 횟수. 화면에 "3주만 더!" 같은 걸 띄우는 용도. */
        int paymentsUntilNextCoupon
) {

    /** 4주 주기로 쿠폰이 나가므로 4의 배수까지 남은 횟수를 계산한다. */
    private static final int COUPON_CYCLE = 4;

    public static SubscriptionResponse from(Subscription subscription) {
        int count = subscription.getConsecutiveCount();
        int remaining = COUPON_CYCLE - (count % COUPON_CYCLE);

        return new SubscriptionResponse(
                subscription.getSubscriptionId(),
                subscription.getStatus(),
                subscription.getMealsPerWeek(),
                subscription.getStartDate(),
                subscription.getNextBillingDate(),
                count,
                remaining);
    }
}

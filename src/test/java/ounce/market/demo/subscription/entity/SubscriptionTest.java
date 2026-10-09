package ounce.market.demo.subscription.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SubscriptionTest {

    @Mock
    private Member member;

    @Test
    void start_isBillableOnStartDate() {
        LocalDate startDate = LocalDate.of(2026, 10, 9);
        Subscription subscription = Subscription.start(member, 5, startDate);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(startDate, subscription.getNextBillingDate());
        assertTrue(subscription.isBillableOn(startDate));
        assertFalse(subscription.isBillableOn(startDate.minusDays(1)));
    }

    @Test
    void deliveryDate_respectsThe23HourCutoff() {
        LocalDate date = LocalDate.of(2026, 10, 9);

        assertEquals(date.plusDays(1),
                Subscription.deliveryDateOf(date.atTime(22, 59, 59)));
        assertEquals(date.plusDays(1),
                Subscription.deliveryDateOf(date.atTime(23, 0)));
        assertEquals(date.plusDays(2),
                Subscription.deliveryDateOf(date.atTime(23, 0, 1)));
    }

    @Test
    void skipThisCycle_rejectsRequestAtOrAfterCutoff() {
        LocalDate date = LocalDate.of(2026, 10, 9);
        Subscription subscription = Subscription.start(member, 5, date);

        SubscriptionException exception = assertThrows(SubscriptionException.class,
                () -> subscription.skipThisCycle(date.plusDays(1).atTime(23, 0)));

        assertEquals(SubscriptionErrorCode.SKIP_DEADLINE_PASSED, exception.getErrorCode());
    }

    @Test
    void pauseUntil_rejectsResumeDateBeforeNextBillingDate() {
        LocalDate date = LocalDate.of(2026, 10, 9);
        Subscription subscription = Subscription.start(member, 5, date);

        SubscriptionException exception = assertThrows(SubscriptionException.class,
                () -> subscription.pauseUntil(date.atTime(10, 0), date));

        assertEquals(SubscriptionErrorCode.INVALID_RESUME_DATE, exception.getErrorCode());
    }

    @Test
    void paymentFailure_schedulesRetryThenCancelsAfterThreeFailedWeeks() {
        LocalDate date = LocalDate.of(2026, 10, 9);
        Subscription subscription = Subscription.start(member, 5, date);

        assertEquals(PaymentFailureResult.RETRY_SCHEDULED,
                subscription.onPaymentFailed(date.atTime(10, 0)));
        assertEquals(SubscriptionStatus.PAYMENT_RETRYING, subscription.getStatus());

        subscription.abandonCycle(date.atTime(12, 0), "카드 결제 실패");
        subscription.onPaymentFailed(date.plusWeeks(1).atTime(10, 0));
        subscription.abandonCycle(date.plusWeeks(1).atTime(12, 0), "카드 결제 실패");
        PaymentFailureResult result = subscription.onPaymentFailed(date.plusWeeks(2).atTime(10, 0));

        assertEquals(PaymentFailureResult.SUBSCRIPTION_CANCELED, result);
        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
        assertFalse(subscription.isBillableOn(date.plusWeeks(2)));
    }

    @Test
    void isOwnedBy_checksMemberOwnership() {
        given(member.getMemberId()).willReturn(7L);
        Subscription subscription = Subscription.start(member, 5, LocalDate.of(2026, 10, 9));

        assertTrue(subscription.isOwnedBy(7L));
        assertFalse(subscription.isOwnedBy(8L));
    }
}

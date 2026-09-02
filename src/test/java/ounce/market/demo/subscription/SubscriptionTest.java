package ounce.market.demo.subscription;

import org.junit.jupiter.api.Test;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.entity.Subscription;
import java.time.LocalDate;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class SubscriptionTest {

    @Test
    void 결제_성공하면_연속횟수가_오르고_결제일이_일주일_밀린다() {
        Subscription sub = Subscription.builder()
                .member(null)
                .mealsPerWeek(5)
                .startDate(LocalDate.of(2026, 9, 1))
                .build();

        int count = sub.onPaymentSucceeded();

        assertThat(count).isEqualTo(1);
        assertThat(sub.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 9, 8));
    }

//    @Test
//    void 스킵해도_연속횟수는_유지된다() {
//        Subscription sub = 구독_생성();
//        sub.onPaymentSucceeded();
//
//        sub.skipNextCycle();
//
//        assertThat(sub.getConsecutiveCount()).isEqualTo(1);
//    }
//
//    @Test
//    void 결제_실패하면_연속횟수가_리셋된다() {
//        Subscription sub = 구독_생성();
//        sub.onPaymentSucceeded();
//        sub.onPaymentSucceeded();
//
//        sub.onPaymentFailed();
//
//        assertThat(sub.getConsecutiveCount()).isZero();
//        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAYMENT_FAILED);
//    }
//
//    @Test
//    void 결제를_4번_연속_성공하면_쿠폰_조건을_만족한다() {
//        Subscription sub = 구독_생성();
//
//        int count = 0;
//        for (int i = 0; i < 4; i++) {
//            count = sub.onPaymentSucceeded();
//        }
//
//        assertThat(count % 4).isZero();
//    }

    @Test
    void 주_3끼는_생성할_수_없다() {
        assertThatThrownBy(() -> Subscription.builder()
                .mealsPerWeek(3)
                .startDate(LocalDate.now())
                .build())
                .isInstanceOf(SubscriptionException.class);
    }
}

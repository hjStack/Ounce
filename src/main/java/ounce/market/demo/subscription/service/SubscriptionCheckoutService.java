package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ounce.market.demo.subscription.runner.SubscriptionBillingRunner;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 사용자가 메뉴를 담고 바로 결제하는 경로. 주로 첫 회차에 쓰인다.
 * <p>
 * 이 클래스에 @Transactional이 없는 게 의도다. PG 호출이 중간에 끼어 있고,
 * 트랜잭션 경계는 SubscriptionBillingService 안에 있다. 여기서 트랜잭션을 열면
 * PG 응답을 기다리는 내내 DB 커넥션과 락을 붙들게 된다.
 */

@Service
@RequiredArgsConstructor
public class SubscriptionCheckoutService {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMenuService menuService;
    private final SubscriptionBillingRunner billingRunner;
    private final Clock clock;


    /**
     * 가입과 동시에 메뉴를 담고 결제한다. 구독의 정상 진입 경로다.
     * <p>
     * 9/3에 이걸 호출하면 9/3이 첫 결제일이 되고, 23시 전이면 9/4 새벽에 배송된다.
     * 결제가 끝나는 즉시 2회차가 열려 9/10 23시까지 메뉴를 바꿀 수 있다.
     * <p>
     * 단계가 셋으로 나뉘어 있고 각각 커밋된다. PG를 호출하는 시점에는
     * 구독과 회차와 메뉴가 이미 DB에 있어야 응답을 못 받아도 대사가 가능하다.
     *
     * @return 구독 ID와 결제 결말
     */

    public CheckoutResult subscribeAndPay(Long memberId, int mealsPerWeek, Map<Long, Integer> selection) {
        // 1) 구독 + 1회차 개설(기본 메뉴)
        Long subscriptionId = subscriptionService.start(memberId, mealsPerWeek);

        // 2) 사용자가 담은 메뉴로 교체. 수량이 안 맞으면 여기서 막히고 결제는 시작도 안 한다.
        menuService.changeMenu(memberId, subscriptionId, selection,List.of());

        // 3) 결제. 배치와 같은 경로를 탄다.
        SubscriptionBillingRunner.ChargeOutcome outcome = billingRunner.chargeNow(subscriptionId, LocalDateTime.now(clock));
        return new CheckoutResult(subscriptionId, outcome);
    }

    public record CheckoutResult(Long subscriptionId, SubscriptionBillingRunner.ChargeOutcome outcome) {
    }

    /**
     * 메뉴를 확정하고 그 자리에서 결제한다.
     *
     * @param selection 상품 ID -> 끼수. 합계가 구독 끼수와 같아야 한다.
     * @return 결제 결말. 화면은 이 값으로 성공·실패·재시도 안내를 나눈다.
     */

    //  고객이 구독 신청하기를 눌러 메뉴를 최종 확정하는 순간 저장·결제된다는 것
    // 이 메서드를 호출할때만 == 메뉴를 저장할떄만 결제
    public SubscriptionBillingRunner.ChargeOutcome checkoutNow(Long memberId, Long subscriptionId, Map<Long, Integer> selection,  List<String> skippedDays) {
        // 1) 메뉴 확정. 실패하면 결제까지 가지 않는다.
        menuService.changeMenu(memberId, subscriptionId, selection, skippedDays == null ? List.of() : skippedDays);

        // 2) 결제. 배치와 같은 3단계를 그대로 탄다.
        //    회차 상태가 DRAFT -> PENDING -> PAID로 넘어가며 배송일이 확정된다.
        return billingRunner.chargeNow(subscriptionId, LocalDateTime.now(clock));
    }

    /**
     * 사용자에게 보여줄 문구.
     * NO_RESPONSE에 "다시 시도해 주세요"를 붙이면 안 된다 — 승인됐을 수도 있어서
     * 재시도가 곧 이중 결제가 된다. 대사 배치가 확인할 때까지 기다리게 해야 한다.
     */

    public String messageOf(SubscriptionBillingRunner.ChargeOutcome outcome) {
        return switch (outcome) {
            case PAID -> "결제가 완료됐습니다. 내일 새벽에 받아보실 수 있어요.";
            case FAILED -> "결제에 실패했습니다. 결제 수단을 확인해 주세요.";
            case NOT_BILLABLE -> "지금은 결제할 수 있는 상태가 아닙니다.";
            case NO_RESPONSE -> "결제 결과를 확인하고 있습니다. 잠시 후 구독 내역에서 확인해 주세요.";
        };
    }
}
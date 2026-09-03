package ounce.market.demo.subscription.entity;

import lombok.Getter;

/**
 * 결제 실패를 반영한 뒤 구독이 어떤 결말을 맞았는지.
 * <p>
 * 예외가 아니라 반환값인 이유가 있다. 결제 실패는 예외 상황이 아니라 정상적으로 예상되는
 * 결과이고, 무엇보다 여기서 예외를 던지면 방금 기록한 실패 이력과 상태 변경이 전부 롤백된다.
 * "실패했다는 사실을 저장하는 트랜잭션"을 실패로 끝낼 수는 없다.
 * <p>
 * 서비스는 이 값으로 알림 문구와 후속 처리를 분기한다.
 */
@Getter
public enum PaymentFailureResult {

    //결제 실패 시 2시간 간격 재시도와 실패한 주 건너뛰기, 3주 연속 실패 시 해지.

    /**
     * 아직 시도 횟수와 마감이 남았다. nextRetryAt에 다시 시도한다.
     * 사용자에게는 결제 수단 변경을 유도하는 알림을 보낸다.
     */
    RETRY_SCHEDULED("결제 재시도 예정", false, false),

    /**
     * 재시도를 다 썼거나 재시도할 가치가 없는 실패였다. 이번 주 회차를 취소하고
     * 다음 주 결제일로 넘어간다. 구독은 살아 있고 배송만 한 주 쉰다.
     */
    WEEK_SKIPPED("이번 주 배송 취소", true, false),

    /** 결제를 못 받고 넘어간 주가 연속 3주. 구독을 해지했다. */
    SUBSCRIPTION_CANCELED("구독 해지", true, true);

    /** 화면과 알림에 쓰는 문구. */
    private final String description;

    /** 이번 주 회차가 취소됐는가. 배송 준비를 멈춰야 하는 신호다. */
    private final boolean cycleAbandoned;

    /** 구독 자체가 끝났는가. */
    private final boolean subscriptionCanceled;

    PaymentFailureResult(String description, boolean cycleAbandoned, boolean subscriptionCanceled) {
        this.description = description;
        this.cycleAbandoned = cycleAbandoned;
        this.subscriptionCanceled = subscriptionCanceled;
    }

    public boolean isCycleAbandoned() {
        return cycleAbandoned;
    }

    public boolean isSubscriptionCanceled() {
        return subscriptionCanceled;
    }

    /** 아직 재시도가 남아 있는가. */
    public boolean isRetryScheduled() {
        return this == RETRY_SCHEDULED;
    }
}

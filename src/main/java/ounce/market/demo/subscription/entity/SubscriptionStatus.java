package ounce.market.demo.subscription.entity;

import lombok.Getter;

/**
 * 구독 상태.
 * <p>
 * 상태 전이표(canTransitionTo 같은 것)를 여기 두지 않았다. 전이 조건이 상태만으로
 * 결정되지 않기 때문이다 — ACTIVE에서 PAUSED로 가려면 23시 마감 전이어야 하고,
 * PAYMENT_RETRYING에서 ACTIVE로 가려면 시도 횟수와 마감이 남아 있어야 한다.
 * 이걸 enum에 절반만 옮겨두면 엔티티와 두 벌이 되고, 둘은 반드시 어긋난다.
 * 전이 판단은 Subscription이 하고, 여기는 상태의 성격만 답한다.
 */
@Getter
public enum SubscriptionStatus {

    /** 정상 구독중. 매주 결제일 23시에 결제가 시도된다. */
    ACTIVE("구독중", true, false),

    /** 결제 실패 후 재시도 대기중. 2시간 간격, 다음날까지 최대 3회. */
    PAYMENT_RETRYING("결제 재시도중", true, false),

    /** 사용자가 직접 걸어둔 쉬어가기. resumeDate 전까지 결제하지 않는다. */
    PAUSED("쉬어가는 중", false, false),

    /** 해지. 사용자 요청 또는 3주 연속 결제 실패. 여기서 나가는 길은 없다. */
    CANCELED("해지됨", false, true);

    /** 화면에 그대로 노출하는 문구. 프론트가 상태값마다 문자열을 따로 들고 있지 않게 한다. */
    private final String description;

    /** 이 상태에서 결제가 나갈 수 있는가. 결제 결과를 반영받을 수 있는 상태이기도 하다. */
    private final boolean chargeable;

    /** 되돌아올 수 없는 상태인가. */
    private final boolean terminal;

    SubscriptionStatus(String description, boolean chargeable, boolean terminal) {
        this.description = description;
        this.chargeable = chargeable;
        this.terminal = terminal;
    }

    /**
     * 결제 성공/실패를 반영받을 수 있는 상태.
     * ACTIVE(첫 시도)와 PAYMENT_RETRYING(재시도) 둘 다 해당한다.
     */
    public boolean isChargeable() {
        return chargeable;
    }

    public boolean isTerminal() {
        return terminal;
    }

    /** 사용자에게 "결제 수단을 바꿔주세요" 배너를 띄워야 하는 상태. */
    public boolean needsPaymentAction() {
        return this == PAYMENT_RETRYING;
    }
}
package ounce.market.demo.payment.entity;

/**
 * 결제 한 건의 결말.
 * <p>
 * 배치와 즉시 결제가 같은 값을 쓴다. 컨트롤러가 이 값으로 화면 분기를 하므로
 * 배치 러너 안에 중첩해 두지 않고 서비스 패키지에 둔다 —
 * 컨트롤러가 배치 클래스를 import하게 되면 계층이 뒤집힌다.
 */
public enum ChargeOutcome {

    PAID,

    /** PG가 명시적으로 거절했다. 사용자가 결제 수단을 바꿔 재시도하면 된다. */
    FAILED,

    /** 결제일이 아니거나 이미 쉬어가기·해지된 구독이다. */
    NOT_BILLABLE,

    /**
     * PG 응답을 못 받았다. 승인 여부 불명이라 사용자에게 재시도를 권하면 안 된다 —
     * 실제로 승인된 건이면 재시도가 곧 이중 결제다.
     */
    NO_RESPONSE
}
package ounce.market.demo.subscription.entity;

public enum CycleStatus {
    /** 메뉴 선택 대기. 마감 시각이 지나면 자동 확정된다. */
    MENU_PENDING,
    /** 메뉴 확정. 결제 대상. */
    CONFIRMED,
    /** 결제 완료. Order와 연결된 상태 */
    PAID,
    /** 결제 실패 */
    FAILED,
    /** 사용자가 이번 주를 건너뜀 */
    SKIPPED
}

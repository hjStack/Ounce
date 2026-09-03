package ounce.market.demo.subscription.entity;

public enum SubscriptionCycleStatus {

    /** 회차는 만들어졌고 결제 결과를 기다린다. PG 호출 전 상태. */
    PENDING,

    /** 결제 성공. 배송 대상. */
    PAID,

    /** 이번 시도는 실패. 재시도가 남아 있다. */
    FAILED,

    /** 재시도까지 전부 실패해서 이번 주를 넘겼다. 배송하지 않는다. */
    ABANDONED,
    DRAFT;

    /** 사용자가 메뉴를 바꿀 수 있는 상태인가. */
    public boolean isMenuEditable() {
        return this == DRAFT;
    }
}
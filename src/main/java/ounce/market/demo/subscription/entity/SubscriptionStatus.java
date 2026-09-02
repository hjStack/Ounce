package ounce.market.demo.subscription.entity;

public enum SubscriptionStatus {
    ACTIVE,    /** 정상 구독 중 */
    PAYMENT_FAILED,     /** 결제 실패로 멈춘 상태. 결제 수단 교체 후 재개 가능 */
    CANCELED   /** 해지 */
}

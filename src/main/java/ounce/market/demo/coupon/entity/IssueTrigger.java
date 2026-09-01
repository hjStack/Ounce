package ounce.market.demo.coupon.entity;


public enum IssueTrigger {
    SIGNUP,                 // 가입 시 자동 발급
    SUBSCRIPTION_STREAK,    // 구독 N회 결제 달성 시
    MANUAL                  // 관리자 수동/벌크
}

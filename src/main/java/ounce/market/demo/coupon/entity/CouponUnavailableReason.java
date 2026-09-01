package ounce.market.demo.coupon.entity;

import ounce.market.demo.coupon.error.CouponErrorCode;

public enum CouponUnavailableReason {
    NONE, ALREADY_USED, EXPIRED, MIN_ORDER_AMOUNT_NOT_MET;

    public CouponErrorCode toErrorCode() {
        return switch (this) {
            case ALREADY_USED -> CouponErrorCode.COUPON_ALREADY_USED;
            case EXPIRED -> CouponErrorCode.COUPON_EXPIRED;
            case MIN_ORDER_AMOUNT_NOT_MET -> CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET;
            case NONE -> throw new IllegalStateException("사용 가능한 쿠폰에는 에러코드가 없습니다.");
        };
    }
}
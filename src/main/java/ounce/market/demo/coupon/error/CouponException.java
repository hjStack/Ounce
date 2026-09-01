package ounce.market.demo.coupon.error;

public class CouponException extends BusinessException {

    public CouponException(CouponErrorCode errorCode) {
        super(errorCode);
    }

    public CouponException(CouponErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    public static CouponException minOrderAmountNotMet(int minOrderAmount, int productAmount) {
        return new CouponException(
                CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET,
                "최소 주문금액 %,d원 이상부터 사용 가능합니다. (현재 %,d원)"
                        .formatted(minOrderAmount, productAmount));
    }
}
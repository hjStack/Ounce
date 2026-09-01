package ounce.market.demo.coupon.dto.response;

import ounce.market.demo.coupon.entity.CouponUnavailableReason;

public record CouponValidationResponse(
        Long couponId,
        String name,
        boolean available,
        int discountAmount,
        int finalAmount,
        int shippingAmount
) {

    public int productDiscountAmount() {
        return discountAmount;
    }

    public int shippingDiscountAmount() {
        return shippingAmount;
    }
}

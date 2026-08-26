package ounce.market.demo.coupon.dto.response;

public record CouponValidationResponse(
        Long couponId,
        boolean available,
        int discountAmount,
        int finalAmount
) {
}

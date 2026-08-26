package ounce.market.demo.coupon.dto.response;

import ounce.market.demo.coupon.entity.Coupon;
import ounce.market.demo.coupon.entity.CouponStatus;
import ounce.market.demo.coupon.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponResponse(
        Long couponId,
        String name,
        CouponStatus status,
        DiscountType discountType,
        int discountAmount,
        Integer maxDiscountAmount,
        int minOrderAmount,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getCouponId(),
                coupon.getName(),
                coupon.getStatus(),
                coupon.getDiscountType(),
                coupon.getDiscountAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                coupon.getIssuedAt(),
                coupon.getExpiresAt(),
                coupon.getUsedAt()
        );
    }
}

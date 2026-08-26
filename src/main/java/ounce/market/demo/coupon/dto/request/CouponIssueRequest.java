package ounce.market.demo.coupon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ounce.market.demo.coupon.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponIssueRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotBlank(message = "쿠폰명은 필수입니다.")
        String name,

        @NotNull(message = "할인 타입은 필수입니다.")
        DiscountType discountType,

        @Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
        int discountAmount,

        Integer maxDiscountAmount,

        @Min(value = 0, message = "최소 주문 금액은 0원 이상이어야 합니다.")
        int minOrderAmount,

        @NotNull(message = "쿠폰 만료일은 필수입니다.")
        LocalDateTime expiresAt
) {
}

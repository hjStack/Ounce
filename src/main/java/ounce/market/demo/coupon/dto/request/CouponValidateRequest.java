package ounce.market.demo.coupon.dto.request;

import jakarta.validation.constraints.Min;

public record CouponValidateRequest(
        @Min(value = 0, message = "주문 금액은 0원 이상이어야 합니다.")
        int orderAmount
) {
}

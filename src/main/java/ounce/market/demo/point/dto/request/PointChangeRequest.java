package ounce.market.demo.point.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ounce.market.demo.point.entity.PointType;

public record PointChangeRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @Min(value = 1, message = "포인트 금액은 1 이상이어야 합니다.")
        int amount,

        @NotNull(message = "포인트 타입은 필수입니다.")
        PointType type,

        String description
) {
}

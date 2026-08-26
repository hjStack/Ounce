package ounce.market.demo.delivery.dto.request;

import jakarta.validation.constraints.NotNull;
import ounce.market.demo.delivery.entity.DeliveryStatus;

public record DeliveryStatusUpdateRequest(
        @NotNull(message = "배송 상태는 필수입니다.")
        DeliveryStatus status
) {
}

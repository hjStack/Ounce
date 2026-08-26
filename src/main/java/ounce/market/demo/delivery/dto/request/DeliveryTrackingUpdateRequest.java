package ounce.market.demo.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeliveryTrackingUpdateRequest(
        @NotBlank(message = "택배사는 필수입니다.")
        String carrier,

        @NotBlank(message = "운송장 번호는 필수입니다.")
        String trackingNumber
) {
}

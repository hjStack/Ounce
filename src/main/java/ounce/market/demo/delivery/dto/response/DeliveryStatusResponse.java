package ounce.market.demo.delivery.dto.response;

import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.delivery.entity.DeliveryStatus;

public record DeliveryStatusResponse(
        Long deliveryId,
        DeliveryStatus status,
        String statusDescription
) {
    public static DeliveryStatusResponse from(Delivery delivery) {
        String statusDescription = delivery.getStatus() == null ? null : delivery.getStatus().getDescription();
        return new DeliveryStatusResponse(
                delivery.getDeliveryId(),
                delivery.getStatus(),
                statusDescription
        );
    }
}

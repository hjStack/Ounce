package ounce.market.demo.delivery.dto.response;

import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.delivery.entity.DeliveryStatus;
import ounce.market.demo.delivery.entity.DeliveryType;

import java.time.LocalDateTime;

public record DeliveryResponse(
        Long deliveryId,
        Long orderId,
        String receiverName,
        String receiverPhone,
        String zipCode,
        String address,
        String addressDetail,
        DeliveryType deliveryType,
        DeliveryStatus status,
        String statusDescription,
        String carrier,
        String trackingNumber,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        Long orderId = delivery.getOrder() == null ? null : delivery.getOrder().getOrderId();
        String statusDescription = delivery.getStatus() == null ? null : delivery.getStatus().getDescription();

        return new DeliveryResponse(
                delivery.getDeliveryId(),
                orderId,
                delivery.getReceiverName(),
                delivery.getReceiverPhone(),
                delivery.getZipCode(),
                delivery.getAddress(),
                delivery.getAddressDetail(),
                delivery.getDeliveryType(),
                delivery.getStatus(),
                statusDescription,
                delivery.getCarrier(),
                delivery.getTrackingNumber(),
                delivery.getShippedAt(),
                delivery.getDeliveredAt()
        );
    }
}

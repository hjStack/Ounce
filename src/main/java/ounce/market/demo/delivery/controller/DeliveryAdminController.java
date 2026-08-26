package ounce.market.demo.delivery.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.delivery.dto.request.DeliveryStatusUpdateRequest;
import ounce.market.demo.delivery.dto.request.DeliveryTrackingUpdateRequest;
import ounce.market.demo.delivery.dto.response.DeliveryResponse;
import ounce.market.demo.delivery.dto.response.DeliveryStatusResponse;
import ounce.market.demo.delivery.service.DeliveryService;

@Tag(name = "10-1. 배송 관리자", description = "배송 상태 및 운송장 관리")
@RestController
@RequestMapping("/api/admin/deliveries")
@RequiredArgsConstructor
public class DeliveryAdminController {

    private final DeliveryService deliveryService;

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<DeliveryStatusResponse> updateStatus(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        return ResponseEntity.ok(deliveryService.updateStatus(deliveryId, request));
    }

    @PatchMapping("/{deliveryId}/tracking")
    public ResponseEntity<DeliveryResponse> updateTracking(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryTrackingUpdateRequest request) {
        return ResponseEntity.ok(deliveryService.updateTracking(deliveryId, request));
    }
}

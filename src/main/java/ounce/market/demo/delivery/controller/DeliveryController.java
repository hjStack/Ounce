package ounce.market.demo.delivery.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.delivery.dto.response.DeliveryResponse;
import ounce.market.demo.delivery.service.DeliveryService;

import java.util.List;

@Tag(name = "10-0. 배송", description = "내 배송 조회")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/deliveries/me")
    public ResponseEntity<List<DeliveryResponse>> getMyDeliveries(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(deliveryService.getMyDeliveries(userDetails.getUsername()));
    }

    @GetMapping("/deliveries/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getMyDelivery(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long deliveryId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(deliveryService.getMyDelivery(userDetails.getUsername(), deliveryId));
    }

    @GetMapping("/orders/{orderId}/delivery")
    public ResponseEntity<DeliveryResponse> getMyOrderDelivery(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(deliveryService.getMyOrderDelivery(userDetails.getUsername(), orderId));
    }
}

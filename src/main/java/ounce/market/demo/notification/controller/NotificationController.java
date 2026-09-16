package ounce.market.demo.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.notification.dto.request.PushSubscriptionRequest;
import ounce.market.demo.notification.dto.request.PushSubscriptionDeleteRequest;
import ounce.market.demo.notification.dto.response.MidnightAlertStatusResponse;
import ounce.market.demo.notification.dto.response.NotificationResponse;
import ounce.market.demo.notification.service.NotificationService;

import java.util.List;

@Tag(name = "07. 미드나이트 알림", description = "미드나이트 알림 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/push-subscriptions")
    @Operation(summary = "웹 푸시 구독 저장")
    public ResponseEntity<Void> savePushSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PushSubscriptionRequest request) {
        notificationService.savePushSubscription(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push-subscriptions")
    @Operation(summary = "웹 푸시 구독 해제")
    public ResponseEntity<Void> deletePushSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PushSubscriptionDeleteRequest request) {
        notificationService.deletePushSubscription(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/midnight")
    @Operation(summary = "미드나이트 알림 신청")
    public ResponseEntity<Void> enableMidnightAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.enableMidnightAlert(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/midnight")
    @Operation(summary = "미드나이트 알림 해제")
    public ResponseEntity<Void> disableMidnightAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.disableMidnightAlert(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/midnight/status")
    @Operation(summary = "미드나이트 알림 신청 상태 조회")
    public ResponseEntity<MidnightAlertStatusResponse> midnightAlertStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(new MidnightAlertStatusResponse(
                notificationService.isMidnightAlertEnabled(userDetails.getUsername())));
    }

    @GetMapping
    @Operation(summary = "내 알림 목록 조회")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.getUsername())
                .stream().map(NotificationResponse::from).toList());
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(userDetails.getUsername(), notificationId);
        return ResponseEntity.noContent().build();
    }
}

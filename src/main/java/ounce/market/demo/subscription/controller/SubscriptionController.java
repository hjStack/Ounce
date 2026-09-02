package ounce.market.demo.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.subscription.dto.SubscriptionCreateRequest;
import ounce.market.demo.subscription.dto.SubscriptionResponse;
import ounce.market.demo.subscription.service.SubscriptionService;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@Tag(name = "구독")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "구독 신청")
    @PostMapping
    public ResponseEntity<Void> subscribe(Principal principal,
                                          @Valid @RequestBody SubscriptionCreateRequest request) {
        Long subscriptionId = subscriptionService.subscribe(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/subscriptions/" + subscriptionId)).build();
    }

    @Operation(summary = "내 구독 조회")
    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(Principal principal) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(principal.getName()));
    }

    @Operation(summary = "내 구독 이력 (해지 포함)")
    @GetMapping("/me/history")
    public ResponseEntity<List<SubscriptionResponse>> getMyHistory(Principal principal) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptionHistory(principal.getName()));
    }

    @Operation(summary = "주당 끼수 변경")
    @PatchMapping("/{subscriptionId}/meals")
    public ResponseEntity<Void> changeMeals(Principal principal,
                                            @PathVariable Long subscriptionId,
                                            @RequestParam int mealsPerWeek) {
        subscriptionService.changeMealsPerWeek(principal.getName(), subscriptionId, mealsPerWeek);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "구독 해지")
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> cancel(Principal principal, @PathVariable Long subscriptionId) {
        subscriptionService.cancel(principal.getName(), subscriptionId);
        return ResponseEntity.noContent().build();
    }
}

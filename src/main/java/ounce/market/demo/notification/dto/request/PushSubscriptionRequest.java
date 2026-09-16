package ounce.market.demo.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dh,
        @NotBlank String auth
) {}

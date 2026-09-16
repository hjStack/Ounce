package ounce.market.demo.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionDeleteRequest(@NotBlank String endpoint) {}

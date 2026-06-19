package ounce.market.demo.timeDeal.dto;

import java.time.LocalDateTime;

public record TimeDealCreateCommand(
        Long productId,
        int discountRate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int maxPurchaseLimit
) {
}

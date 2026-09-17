package ounce.market.demo.timeDeal.dto;

import ounce.market.demo.timeDeal.entity.DealStatus;
import ounce.market.demo.timeDeal.entity.TimeDeal;

import java.time.LocalDateTime;

public record TimeDealAdminResponse(
        Long timeDealId,
        Long productId,
        String productName,
        int discountRate,
        int maxPurchaseLimit,
        LocalDateTime startTime,
        LocalDateTime endTime,
        DealStatus status
) {
    public static TimeDealAdminResponse from(TimeDeal deal) {
        return new TimeDealAdminResponse(
                deal.getTimeDealId(),
                deal.getProduct().getProductId(),
                deal.getProduct().getName(),
                deal.getDiscountRate(),
                deal.getMaxPurchaseLimit(),
                deal.getStartTime(),
                deal.getEndTime(),
                deal.getStatus()
        );
    }
}

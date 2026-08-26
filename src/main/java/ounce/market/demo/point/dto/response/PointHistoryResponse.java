package ounce.market.demo.point.dto.response;

import ounce.market.demo.point.entity.PointHistory;
import ounce.market.demo.point.entity.PointType;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long pointHistoryId,
        Long orderId,
        int amount,
        PointType type,
        String typeDescription,
        String description,
        int balanceAfter,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory pointHistory) {
        Long orderId = pointHistory.getOrder() == null ? null : pointHistory.getOrder().getOrderId();
        String typeDescription = pointHistory.getType() == null ? null : pointHistory.getType().getDescription();

        return new PointHistoryResponse(
                pointHistory.getPointHistoryId(),
                orderId,
                pointHistory.getAmount(),
                pointHistory.getType(),
                typeDescription,
                pointHistory.getDescription(),
                pointHistory.getBalanceAfter(),
                pointHistory.getCreatedAt()
        );
    }
}

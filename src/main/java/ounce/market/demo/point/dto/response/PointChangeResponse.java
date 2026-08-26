package ounce.market.demo.point.dto.response;

public record PointChangeResponse(
        Long memberId,
        int amount,
        int balanceAfter
) {
}

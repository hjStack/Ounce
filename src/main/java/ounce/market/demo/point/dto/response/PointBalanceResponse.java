package ounce.market.demo.point.dto.response;

import ounce.market.demo.member.entity.Member;

public record PointBalanceResponse(
        Long memberId,
        int point
) {
    public static PointBalanceResponse from(Member member) {
        return new PointBalanceResponse(member.getMemberId(), member.getPoint());
    }
}

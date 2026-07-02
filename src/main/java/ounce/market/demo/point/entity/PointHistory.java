package ounce.market.demo.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {
    @Id
    @GeneratedValue
    private Long pointHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    private int amount; // 사용/환불은 음수, 충전/적립은 양수
    @Enumerated(EnumType.STRING)
    private PointType type;

    @Builder
    public PointHistory(Member member, int amount, PointType type) {
        this.member = member;
        this.amount = amount;
        this.type = type;
    }
}

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

    // todo 회원가입하면 1000포인트 주는 프론트 보여주기
    // 그래서 지금 상태를 정리하면, 이메일로 가입하면 1000포인트를 받고, 구글로 가입하면 0을 받아요.
    // 나중에 구글 가입에도 동일하게 주려면 OAuth 회원 생성하는 곳에도 같은 지급 로직을 넣어야 해요.
    // 지금 급한 건 아니고, 원할 때 그 코드 보여주시면 맞춰드릴게요.
}

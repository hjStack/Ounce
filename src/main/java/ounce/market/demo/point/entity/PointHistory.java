package ounce.market.demo.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.order.entity.Order;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {
    @Id
    @GeneratedValue
    private Long pointHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int amount; // 사용/환불은 음수, 충전/적립은 양수
    @Enumerated(EnumType.STRING)
    private PointType type;

    private String description;

    private int balanceAfter;

    private LocalDateTime expiresAt;

    @Builder
    public PointHistory(Member member, Order order, int amount, PointType type,
                        String description, int balanceAfter, LocalDateTime expiresAt) {
        this.member = member;
        this.order = order;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.balanceAfter = balanceAfter;
        this.expiresAt = expiresAt;
    }


    public static PointHistory deduct(Member member, int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("회수 포인트는 0보다 커야 합니다.");
        }

        return PointHistory.builder()
                .member(member)
                .amount(-amount)
                .balanceAfter(member.getPoint())
                .type(PointType.REWARD_CANCEL)
                .description("관리자 리뷰 삭제에 따른 포인트 회수")
                .build();
    }


    // todo 회원가입하면 1000포인트 주는 프론트 보여주기 -> 완료
    // 구글 회원가입해도 1000포인트
}

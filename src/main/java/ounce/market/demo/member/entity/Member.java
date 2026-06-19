package ounce.market.demo.member.entity;

import jakarta.persistence.*;
import lombok.*;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.exception.InsufficientPointException;
import ounce.market.demo.notification.entity.Notification;

@Entity
@Getter
@NoArgsConstructor()
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long memberId;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 오직 회원만 주문가능하므로 포인트는 무조건 있음
    @Column(nullable = false)
    private int point;

    public void usePoint(int amount) {
        if (this.point < amount) {
            throw new InsufficientPointException(this.memberId);
        }
        this.point -= amount;
    }

    public void chargePoint(int amount) {
        this.point += amount;
    }
}

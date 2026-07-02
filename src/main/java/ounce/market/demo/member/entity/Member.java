package ounce.market.demo.member.entity;

import jakarta.persistence.*;
import lombok.*;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.common.global.InsufficientPointException;

@Entity
@Getter
@NoArgsConstructor()
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    // 오직 회원만 주문가능하므로 포인트는 무조건 있음
    @Column(nullable = false)
    private int point;

}

package ounce.market.demo.member.entity;

import jakarta.persistence.*;
import lombok.*;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.common.global.InsufficientPointException;

@Entity
@Getter
@NoArgsConstructor
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

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    // 오직 회원만 주문가능하므로 포인트는 무조건 있음
    @Column(nullable = false)
    private int point;

    public void deductPoint(int amount) {
        // 1. 비정상적인 마이너스 금액 차감 시도 방어 (해킹/버그 원천 차단)
        if (amount < 0) {
            throw new IllegalArgumentException("차감할 포인트는 0보다 커야 합니다.");
        }

        // 2. 이중 체크: Service 단에서도 검사하지만, 객체 스스로도 한 번 더 자신의 상태를 검증합니다.
        if (this.point < amount) {
            throw new IllegalArgumentException("결제 오류: 보유 포인트가 부족합니다. (현재 잔액: " + this.point + "원)");
        }

        // 3. 안전하게 포인트 차감
        this.point -= amount;
    }

}

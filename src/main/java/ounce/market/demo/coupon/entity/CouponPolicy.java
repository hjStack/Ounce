package ounce.market.demo.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.coupon.error.CouponErrorCode;
import ounce.market.demo.coupon.error.CouponException;
import ounce.market.demo.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupon_policy")
public class CouponPolicy extends BaseEntity {

    // 4주 연속 구독하면 관리자가 주는 3000원 쿠폰

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @Column(nullable = false, unique = true, length = 50)
    private String code;
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    private int discountAmount;
    private Integer maxDiscountAmount;

    /** 배송비 제외 상품금액 기준 */
    private int minOrderAmount;

    // --- 발급 조건 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueTrigger issueTrigger; // 카프카 컨슈머가 분기할 기준점

    /** SUBSCRIPTION_STREAK일 때 달성 회차 (4) */
    // 4주 연속 구독시 3천원 쿠폰
    private Integer triggerThreshold=4; // todo ? 구독한 채로 4주차가 되면 구독 감사 쿠폰  : 3천원 할인 ?

    private LocalDateTime issueStartAt;
    private LocalDateTime issueEndAt;

    /** 회원당 최대 발급 수. 0이면 무제한 */
    private int maxIssuePerMember; // todo 이거도 정하기

    /** 전체 수량. null이면 무제한 */
    private Integer totalQuantity; // todo 이거도 정하기
    private int issuedCount;

    // --- 유효기간 (택1) ---
    private Integer validDays;

    private boolean active;

    public boolean isIssuable(LocalDateTime now) {
        if (!active) return false;
        if (issueStartAt != null && now.isBefore(issueStartAt)) return false;
        if (issueEndAt != null && now.isAfter(issueEndAt)) return false;
        return totalQuantity == null || issuedCount < totalQuantity;
    }

    public Coupon issueTo(Member member, String issueKey, LocalDateTime now) {
        if (!isIssuable(now)) {
            throw new CouponException(CouponErrorCode.COUPON_ISSUE_CLOSED);
        }
        return Coupon.builder()
                .member(member)
                .policy(this)
                .issueKey(issueKey)
                .name(name)
                .discountType(discountType)
                .discountAmount(discountAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .minOrderAmount(minOrderAmount)
                .issuedAt(now)
                .expiresAt(resolveExpiresAt(now))
                .build();
    }

    private LocalDateTime resolveExpiresAt(LocalDateTime now) {
//        if (fixedExpiresAt != null) return fixedExpiresAt;
        if (validDays != null) return now.plusDays(validDays).toLocalDate().atTime(23, 59, 59);
        return null;
    }
}
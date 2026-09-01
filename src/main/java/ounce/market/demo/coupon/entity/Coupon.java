package ounce.market.demo.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.coupon.error.CouponErrorCode;
import ounce.market.demo.coupon.error.CouponException;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.order.entity.Order;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "coupon",
        uniqueConstraints = {
                // 카프카 중복 소비 방어선
                @UniqueConstraint(name = "uk_coupon_issue_key", columnNames = "issue_key"),
                // 한 주문에 쿠폰 하나
                @UniqueConstraint(name = "uk_coupon_order", columnNames = "order_id")
        },
        indexes = @Index(name = "idx_coupon_member_status", columnList = "member_id, status")
)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Version
    private Long version;

    /** 멱등키. 예: "SUB_STREAK:member=12:cycle=4" */
    @Column(name = "issue_key", length = 120)
    private String issueKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private CouponPolicy policy;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    private int discountAmount;
    private int finalAmount;
    private Integer maxDiscountAmount;
    private int minOrderAmount;

    private int shipppingAmount;

    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Builder
    public Coupon(Member member, CouponPolicy policy, String issueKey, String name,
                  DiscountType discountType, int discountAmount, Integer maxDiscountAmount,
                  int minOrderAmount, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        validatePolicy(discountType, discountAmount, minOrderAmount);
        this.member = member;
        this.policy = policy;
        this.issueKey = issueKey;
        this.name = name;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.status = CouponStatus.AVAILABLE;
    }

    private static void validatePolicy(DiscountType type, int amount, int minOrderAmount) {
        if (type == null || minOrderAmount < 0) {
            throw new CouponException(CouponErrorCode.INVALID_COUPON_POLICY);
        }
        boolean invalid = switch (type) {
            case FIXED -> amount <= 0;
            case PERCENT -> amount <= 0 || amount > 100;
            case FREE_SHIPPING -> amount != 0;
        };
        if (invalid) {
            throw new CouponException(CouponErrorCode.INVALID_COUPON_POLICY);
        }
    }

    // ===== 검증 =====

    public boolean isOwnedBy(Long memberId) {
        return member != null && member.getMemberId().equals(memberId);
    }

    public CouponUnavailableReason validateFor(int productAmount, LocalDateTime now) {
        if (status == CouponStatus.USED) return CouponUnavailableReason.ALREADY_USED;
        if (status == CouponStatus.EXPIRED) return CouponUnavailableReason.EXPIRED;
        if (expiresAt != null && !expiresAt.isAfter(now)) return CouponUnavailableReason.EXPIRED;
        if (productAmount < minOrderAmount) return CouponUnavailableReason.MIN_ORDER_AMOUNT_NOT_MET;
        return CouponUnavailableReason.NONE;
    }

    public boolean isAvailableFor(int productAmount, LocalDateTime now) {
        return validateFor(productAmount, now) == CouponUnavailableReason.NONE;
    }

    // ===== 할인 계산 =====

    public int calculateProductDiscount(int productAmount, LocalDateTime now) {
        if (!isAvailableFor(productAmount, now)) return 0;
        int calculated = switch (discountType) {
            case FIXED -> discountAmount;
            case PERCENT -> productAmount * discountAmount / 100;
            case FREE_SHIPPING -> 0;
        };
        if (discountType == DiscountType.PERCENT && maxDiscountAmount != null) {
            calculated = Math.min(calculated, maxDiscountAmount);
        }
        return Math.min(calculated, productAmount);
    }

    public int calculateShippingDiscount(int productAmount, int shippingFee, LocalDateTime now) {
        if (discountType != DiscountType.FREE_SHIPPING) return 0;
        if (!isAvailableFor(productAmount, now)) return 0;
        int limit = (maxDiscountAmount != null) ? maxDiscountAmount : shippingFee;
        return Math.min(shippingFee, limit);
    }

    // ===== 상태 변경 =====

    public void use(Order order, int productAmount, LocalDateTime now) {
        CouponUnavailableReason reason = validateFor(productAmount, now);
        if (reason != CouponUnavailableReason.NONE) {
            throw new CouponException(reason.toErrorCode());
        }
        if (this.order != null) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_USED);
        }
        this.order = order;
        this.status = CouponStatus.USED;
        this.usedAt = now;
    }

    /** 주문 취소/결제 실패 시 복구. 멱등. */
    public void cancelUse(LocalDateTime now) {
        if (this.status != CouponStatus.USED) return;
        this.order = null;
        this.usedAt = null;
        this.status = (expiresAt != null && !expiresAt.isAfter(now))
                ? CouponStatus.EXPIRED : CouponStatus.AVAILABLE;
    }

    public void expire() {
        if (this.status == CouponStatus.USED) {
            throw new CouponException(CouponErrorCode.CANNOT_EXPIRE_USED_COUPON);
        }
        this.status = CouponStatus.EXPIRED;
    }

    public int calculateDiscountAmount(int totalAmount) {
        return totalAmount;
    }
}
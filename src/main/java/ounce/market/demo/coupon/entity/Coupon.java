package ounce.market.demo.coupon.entity;

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
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private int discountAmount;

    private Integer maxDiscountAmount;

    private int minOrderAmount;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true) // 쿠폰을 쓸수도 있고 안쓸수도
    private Order order;  // 1개의 주문당 1개의 쿠폰만 사용가능함

    @Builder
    public Coupon(Member member, String name, DiscountType discountType, int discountAmount,
                  Integer maxDiscountAmount, int minOrderAmount, LocalDateTime expiresAt) {
        this.member = member;
        this.name = name;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.status = CouponStatus.AVAILABLE;
    }

    public boolean isAvailableFor(int orderAmount) {
        if (status != CouponStatus.AVAILABLE) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return orderAmount >= minOrderAmount;
    }

    public int calculateDiscountAmount(int orderAmount) {
        if (!isAvailableFor(orderAmount) || discountType == null) {
            return 0;
        }

        int calculatedAmount = switch (discountType) {
            case FIXED -> discountAmount;
            case PERCENT -> orderAmount * discountAmount / 100;
        };

        if (discountType == DiscountType.PERCENT && maxDiscountAmount != null) {
            calculatedAmount = Math.min(calculatedAmount, maxDiscountAmount);
        }

        return Math.min(calculatedAmount, orderAmount);
    }

    public void use(Order order) {
        use(order, order.getTotalAmount());
    }

    public void use(Order order, int orderAmount) {
        if (!isAvailableFor(orderAmount)) {
            throw new IllegalArgumentException("사용할 수 없는 쿠폰입니다.");
        }
        this.order = order;
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void expire() {
        if (this.status == CouponStatus.USED) {
            throw new IllegalArgumentException("이미 사용된 쿠폰은 만료 처리할 수 없습니다.");
        }
        this.status = CouponStatus.EXPIRED;
    }
}

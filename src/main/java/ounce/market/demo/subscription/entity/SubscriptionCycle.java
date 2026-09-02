package ounce.market.demo.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.subscription.Exception.SubscriptionException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 구독의 주차별 회차. 매주 하나씩 쌓이는 이력이다.
 * 결제가 성공하면 Order 하나와 연결되어, 이후 배송·재고 흐름은 기존 주문 로직을 그대로 탄다.
 */

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscription_cycle",
        uniqueConstraints = {
                // 같은 구독의 같은 회차가 두 번 생기지 않게 한다.
                // 배치가 중복 실행돼도 여기서 막힌다.
                @UniqueConstraint(
                        name = "uk_cycle_subscription_number",
                        columnNames = {"subscription_id", "cycle_number"}),
                @UniqueConstraint(name = "uk_cycle_order", columnNames = "order_id")
        },
        indexes = @Index(name = "idx_cycle_status_delivery",
                columnList = "status, delivery_date")
)

// 4주 연속 구독시 3천원 쿠폰 주기 위함
public class SubscriptionCycle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleStatus status;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(nullable = false)
    private int mealsCount;

    /** 사용자가 직접 골랐는지, 마감으로 자동 확정됐는지 */
    private boolean autoConfirmed;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDateTime paidAt;
    private String failureReason;

    private static final int MENU_DEADLINE_DAYS_BEFORE = 2;
    private static final int MENU_DEADLINE_HOUR = 23;

    @Builder
    public SubscriptionCycle(Subscription subscription, int cycleNumber,
                             int mealsCount, LocalDate deliveryDate) {
        this.subscription = subscription;
        this.cycleNumber = cycleNumber;
        this.mealsCount = mealsCount;
        this.deliveryDate = deliveryDate;
        this.status = CycleStatus.MENU_PENDING;
    }

    // ===== 메뉴 확정 =====
    /** 사용자가 직접 메뉴를 확정 */
    public void confirmMenu(LocalDateTime now) {
        requireStatus(CycleStatus.MENU_PENDING);
        if (!now.isBefore(menuDeadline())) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_DEADLINE_PASSED);
        }
        this.status = CycleStatus.CONFIRMED;
        this.autoConfirmed = false;
    }

    /** 마감 배치가 기본 구성으로 자동 확정. 멱등. */
    public void autoConfirm() {
        if (this.status != CycleStatus.MENU_PENDING) {
            return;
        }
        this.status = CycleStatus.CONFIRMED;
        this.autoConfirmed = true;
    }

    // ===== 결제 =====

    public void markPaid(Order order, LocalDateTime now) {
        requireStatus(CycleStatus.CONFIRMED);
        this.order = order;
        this.status = CycleStatus.PAID;
        this.paidAt = now;
    }

    public void markFailed(String reason) {
        requireStatus(CycleStatus.CONFIRMED);
        this.status = CycleStatus.FAILED;
        // 실패 사유는 그대로 노출하면 안 되므로 코드 수준으로만 남긴다.
        this.failureReason = reason;
    }

    /** 사용자가 이번 주를 건너뜀 */
    public void skip() {
        if (this.status != CycleStatus.MENU_PENDING && this.status != CycleStatus.CONFIRMED) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = CycleStatus.SKIPPED;
    }

    public boolean isPayable() {
        return this.status == CycleStatus.CONFIRMED;
    }

    public LocalDateTime menuDeadline() {
        return deliveryDate.minusDays(MENU_DEADLINE_DAYS_BEFORE).atTime(23, 0);
    }

    private void requireStatus(CycleStatus expected) {
        if (this.status != expected) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}

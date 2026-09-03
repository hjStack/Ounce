package ounce.market.demo.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 구독 1주치 회차.
 * <p>
 * 수명은 이렇다. 결제 3일 전(금요일)에 DRAFT로 열려 사용자가 메뉴를 고르고,
 * 결제일 23시에 PENDING으로 확정되며 그 시점의 메뉴와 금액이 고정된다.
 * 그 뒤 PG 결과에 따라 PAID / FAILED / ABANDONED로 간다.
 * <p>
 * 회차를 결제 시점이 아니라 메뉴 오픈 시점에 만드는 이유가 이거다.
 * 사용자는 결제가 일어나기 전에 이번 주 박스에 뭐가 들어갈지 정해야 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscription_cycle",
        uniqueConstraints = {
                // 같은 회차가 두 번 생기면 이중 결제다. DB에서 막는다.
                @UniqueConstraint(name = "uk_cycle_subscription_number",
                        columnNames = {"subscription_id", "cycle_number"})
        },
        indexes = @Index(name = "idx_cycle_delivery", columnList = "delivery_date, status")
)
public class SubscriptionCycle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionCycleStatus status;

    /** 회차 시점의 끼수 스냅샷. 담긴 항목 수량 합계가 이 값과 같아야 한다. */
    @Column(nullable = false)
    private int mealsPerWeek;

    /** 확정 시점에 계산된 청구 금액. DRAFT 동안은 항목 합계로 미리보기만 한다. */
    @Column(nullable = false)
    private Long amount;

    /** 결제 예정일. 이 날 23시에 확정된다. */
    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    /** 새벽 배송일. 확정 시점에 결정되므로 DRAFT 동안은 비어 있다. */
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(nullable = false)
    private int attemptCount;

    private String lastFailureCode;

    private String paymentKey;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<SubscriptionCycleItem> items = new ArrayList<>();

    private SubscriptionCycle(Subscription subscription, int cycleNumber,
                              int mealsPerWeek, LocalDate billingDate) {
        this.subscription = subscription;
        this.cycleNumber = cycleNumber;
        this.mealsPerWeek = mealsPerWeek;
        this.billingDate = billingDate;
        this.status = SubscriptionCycleStatus.DRAFT;
        this.amount = 0L;
        this.attemptCount = 0;
    }

    /** 메뉴 선택 기간 개설. 결제 3일 전에 배치가 호출한다. */
    public static SubscriptionCycle openForMenu(Subscription subscription, int cycleNumber,
                                                int mealsPerWeek, LocalDate billingDate) {
        return new SubscriptionCycle(subscription, cycleNumber, mealsPerWeek, billingDate);
    }

    // ===== 메뉴 =====

    /**
     * 메뉴 교체. 기존 선택을 통째로 갈아끼운다.
     * 수량 합계가 끼수와 맞아야 한다 — 5끼 구독인데 3끼만 담고 결제되면 그대로 손실이다.
     */
    public void changeMenu(List<MenuLine> lines) {
        if (!status.isMenuEditable()) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_NOT_EDITABLE);
        }

        List<MenuLine> mergedLines = mergeDuplicateProducts(lines);

        int total = mergedLines.stream().mapToInt(MenuLine::quantity).sum();
        if (total != mealsPerWeek) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_QUANTITY_MISMATCH,
                    "expected=%d actual=%d".formatted(mealsPerWeek, total));
        }

        Map<Long, SubscriptionCycleItem> existing = new LinkedHashMap<>();
        for (SubscriptionCycleItem item : items) {
            existing.put(item.getProductId(), item);
        }

        for (MenuLine line : mergedLines) {
            SubscriptionCycleItem item = existing.remove(line.productId());

            if (item != null) {
                item.replaceWith(line.productName(), line.unitPrice(), line.quantity());
            } else {
                items.add(new SubscriptionCycleItem(
                        this, line.productId(), line.productName(), line.unitPrice(), line.quantity()));
            }
        }

        items.removeAll(existing.values());
        this.amount = this.items.stream().mapToLong(SubscriptionCycleItem::subtotal).sum();
    }

    private List<MenuLine> mergeDuplicateProducts(List<MenuLine> lines) {
        Map<Long, MenuLine> merged = new LinkedHashMap<>();

        for (MenuLine line : lines) {
            validateMenuLine(line);

            MenuLine previous = merged.get(line.productId());
            if (previous == null) {
                merged.put(line.productId(), line);
                continue;
            }

            merged.put(line.productId(), new MenuLine(
                    line.productId(),
                    line.productName(),
                    line.unitPrice(),
                    previous.quantity() + line.quantity()));
        }

        return new ArrayList<>(merged.values());
    }

    private void validateMenuLine(MenuLine line) {
        if (line.productId() == null || line.quantity() <= 0 || line.unitPrice() == null || line.unitPrice() < 0) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_QUANTITY_MISMATCH);
        }
    }


    /**
     * 결제일을 옮긴다. 쉬어가기로 구독의 다음 결제일이 밀리면
     * 열려 있는 회차의 날짜도 같이 밀어야 한다. 안 그러면 회차가 지난 날짜를 들고 남는다.
     */
    public void rescheduleTo(LocalDate billingDate) {
        if (status != SubscriptionCycleStatus.DRAFT) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.billingDate = billingDate;
    }

    public boolean hasMenu() {
        return !items.isEmpty();
    }

    /**
     * 결제 직전 확정. 메뉴가 닫히고 배송일과 금액이 고정된다.
     * 메뉴를 안 고른 채로 여기 오면 안 된다 — 배치가 기본 구성을 먼저 채워야 한다.
     */
    public void confirmForBilling(LocalDateTime now) {
        if (status != SubscriptionCycleStatus.DRAFT) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (!hasMenu()) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_NOT_SELECTED,
                    "cycleNumber=" + cycleNumber);
        }
        this.status = SubscriptionCycleStatus.PENDING;
        this.deliveryDate = Subscription.deliveryDateOf(now);
        this.amount = this.items.stream().mapToLong(SubscriptionCycleItem::subtotal).sum();
    }

    // ===== 결제 =====

    /** PG 호출 직전에 시도 횟수를 올린다. 멱등키 생성에 쓰인다. */
    public int beginAttempt() {
        return ++this.attemptCount;
    }

    /** 이미 결제가 확정된 회차인가. 구독 상태를 두 번 미는 것을 막는 최종 가드다. */
    public boolean isPaid() {
        return this.status == SubscriptionCycleStatus.PAID;
    }

    public void markPaid(String paymentKey, LocalDate deliveryDate) {
        this.status = SubscriptionCycleStatus.PAID;
        this.paymentKey = paymentKey;
        this.deliveryDate = deliveryDate;
        this.lastFailureCode = null;
    }

    public void markFailed(String failureCode) {
        this.status = SubscriptionCycleStatus.FAILED;
        this.lastFailureCode = failureCode;
    }

    /** 재시도까지 다 실패해서 이번 주를 통째로 넘긴 경우. */
    public void markAbandoned(String failureCode) {
        this.status = SubscriptionCycleStatus.ABANDONED;
        this.lastFailureCode = failureCode;
    }

    public void closeOnCancel() {
        if (status != SubscriptionCycleStatus.DRAFT) {
            return;
        }
        this.status = SubscriptionCycleStatus.ABANDONED;
        this.lastFailureCode = "SUBSCRIPTION_CANCELED";
    }

    /** 메뉴 한 줄. 상품 정보는 서비스가 조회해 스냅샷으로 넘긴다. */
    public record MenuLine(Long productId, String productName,
                           Long unitPrice, int quantity) {
    }
}
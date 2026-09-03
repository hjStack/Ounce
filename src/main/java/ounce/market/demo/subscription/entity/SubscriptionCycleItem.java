package ounce.market.demo.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;

/**
 * 회차에 담긴 밀키트 한 종류.
 * <p>
 * Product 엔티티를 연관으로 걸지 않고 productId와 이름·가격을 복사해 둔다.
 * 상품 가격이 다음 주에 바뀌어도 이미 결제된 회차의 청구 금액은 그대로여야 하고,
 * 상품이 단종돼도 지난 배송 내역은 남아야 한다. 주문 도메인과 같은 이유다.
 * <p>
 * 스냅샷을 뜨는 시점은 "결제"가 아니라 "메뉴를 담은 때"다. 9/4에 담고 9/10에 결제되면
 * 9/4 가격으로 청구된다. 담을 때 본 가격으로 받는 셈이라 사용자에게 유리하고 분쟁도 없지만,
 * 그 사이 가격이 오르면 차액은 회사가 부담한다. 결제 시점 가격으로 받으려면
 * confirmForBilling에서 단가를 다시 읽어야 하고, 그때는 "금액이 바뀌었습니다" 고지가 필요하다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscription_cycle_item",
        uniqueConstraints = {
                // 같은 회차에 같은 상품이 두 줄로 들어가면 수량 합계가 맞아도 화면과 정산이 어긋난다.
                @UniqueConstraint(name = "uk_cycle_item_product",
                        columnNames = {"cycle_id", "product_id"})
        }
)
public class SubscriptionCycleItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private SubscriptionCycle cycle;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 담은 시점의 상품명. */
    @Column(nullable = false, length = 100)
    private String productName;

    /** 담은 시점의 단가. */
    @Column(nullable = false)
    private Long unitPrice;

    /** 이 밀키트를 몇 끼분 담았는지. */
    @Column(nullable = false)
    private int quantity;

    SubscriptionCycleItem(SubscriptionCycle cycle, Long productId, String productName,
                          Long unitPrice, int quantity) {
        // 0끼짜리 줄이 섞이면 합계 검증은 통과하는데 실제로는 아무것도 안 담긴 상품이 남는다.
        if (quantity <= 0) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_QUANTITY_MISMATCH,
                    "productId=%d quantity=%d".formatted(productId, quantity));
        }
        if (unitPrice < 0) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_QUANTITY_MISMATCH,
                    "productId=%d unitPrice=%d".formatted(productId, unitPrice));
        }
        this.cycle = cycle;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }


    void replaceWith(String productName, Long unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_QUANTITY_MISMATCH,
                    "productId=%d quantity=%d".formatted(productId, quantity));
        }
        if (unitPrice == null || unitPrice < 0) {
            throw new SubscriptionException(SubscriptionErrorCode.MENU_QUANTITY_MISMATCH,
                    "productId=%d unitPrice=%d".formatted(productId, unitPrice));
        }

        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long subtotal() {
        return unitPrice * quantity;
    }
}
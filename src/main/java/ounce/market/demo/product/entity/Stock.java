package ounce.market.demo.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.product.exception.OutOfStockException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue
    private Long stockId;

    private int quantity;

    @Version // 🔥 10시 트래픽을 방어할 낙관적 락의 핵심!
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private  Product product;

    @Builder
    public Stock(int quantity, Product product) {
        this.quantity = quantity;
        this.product = product;
    }

    // 같은 row를 동시에 건드리는 두 트랜잭션 중 하나는 커밋 시점에 OptimisticLockingFailureException으로 튕겨나감 -> 호출 측에서 재시도
    public void decrease(int amount) {
        if (this.quantity < amount) {
            throw new OutOfStockException(this.product != null ? this.product.getProductId() : null);
        }
        this.quantity -= amount;
    }
}

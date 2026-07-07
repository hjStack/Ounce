package ounce.market.demo.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String productCode;
    private String name;

    private Long basePrice;
    // 할인된 가격은 비지니스 로직으로

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private String description;
    private String imageUrl;

    private int stock;

    @Builder
    public Product(String productCode, String name, Long basePrice,
                   int stock, String description, String imageUrl) {
        this.productCode = productCode;
        this.name = name;
        this.basePrice = basePrice;
        this.stock=stock;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.PREPARING;
    }

    public long getSalePrice() {
        // TODO: 할인 로직 추가 지점 (지금은 정가)
        return this.basePrice;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }


    /** ✅ 재고 차감 (부족하면 예외 → 트랜잭션 롤백) */
    public void decreaseStock(int quantity) {

        // todo 이렇게 하면 동시성 이슈 발생 -> 비관적 락으로 수정
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다.");
        }
        if (this.stock < quantity) {
            throw new IllegalArgumentException(
                    name + " 상품의 재고가 부족합니다. (남은 재고: " + this.stock + "개)");
        }
        this.stock -= quantity;
    }
}

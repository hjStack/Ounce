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
    @GeneratedValue
    private Long productId;

    private String productCode;
    private String name;

    private Long basePrice;
    // 할인된 가격은 비지니스 로직으로

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private String description;
    private String imageUrl;

    @Builder
    public Product(String productCode, String name, Long basePrice, String description, String imageUrl) {
        this.productCode = productCode;
        this.name = name;
        this.basePrice = basePrice;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.PREPARING;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }
}

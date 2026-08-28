package ounce.market.demo.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ProductCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 매핑 테이블 자체의 식별자(PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private ProductCategory(Product product, Category category) {
        this.product = product;
        this.category = category;
    }

    public static ProductCategory of(Product product, Category category) {
        return ProductCategory.builder()
                .product(product)
                .category(category)
                .build();
    }
}
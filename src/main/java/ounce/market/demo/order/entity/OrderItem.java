package ounce.market.demo.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.global.GlobalExceptionHandler;
import ounce.market.demo.product.entity.Product;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Builder
    public OrderItem(int price, Product product, int quantity) {
        this.price = price;
        this.product = product;
        this.quantity = quantity;
    }

    void assignOrder(Order order) {    // package-private → 외부 노출 안 됨
        this.order = order;
    }
}

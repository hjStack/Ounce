package ounce.market.demo.product.dto.response;

import lombok.Getter;
import ounce.market.demo.product.entity.Product;

@Getter
public class ProductResponse {

    private final Long productId;
    private final String name;
    private final Long basePrice;
    private final long salePrice;
    private final String description;
    private final String imageUrl;
    private final int stock;
    private final String status;

    private ProductResponse(Product product) {
        this.productId = product.getProductId();
        this.name = product.getName();
        this.basePrice = product.getBasePrice();
        this.salePrice = product.getSalePrice();
        this.description = product.getDescription();
        this.imageUrl = product.getImageUrl();
        this.stock = product.getStock();
        this.status = product.getStatus().name();
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product);
    }
}
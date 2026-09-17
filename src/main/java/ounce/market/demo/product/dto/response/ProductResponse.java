package ounce.market.demo.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import ounce.market.demo.common.service.ImageUrlResolver;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;

@Getter
@Builder
public class ProductResponse {

    private final Long productId;
    private final String name;
    private final Long basePrice;
    private final long salePrice;
    private final long discountPercent;
    private final String description;
    private final String imageUrl;
    private final int stock;
    private final ProductStatus status;
    private final Integer subscriptionDiscountPercent;

    public ProductResponse(Long productId, String name, Long basePrice, long salePrice, long discountPercent, String description,
                           String imageUrl, int stock, ProductStatus status,Integer subscriptionDiscountPercent) {
        this.productId = productId;
        this.name = name;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.discountPercent = discountPercent;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.status = status;
        this.subscriptionDiscountPercent=subscriptionDiscountPercent;
    }

    public static ProductResponse from(Product product, ImageUrlResolver imageUrlResolver) {
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getBasePrice(),
                product.getSalePrice(),
                product.getDiscountPercent(),
                product.getDescription(),
                imageUrlResolver.toUrl(product.getImageUrl()),   // 키 → CloudFront URL
                product.getStock(),
                product.getStatus(),
                product.getSubscriptionDiscountPercent()
        );
    }
}
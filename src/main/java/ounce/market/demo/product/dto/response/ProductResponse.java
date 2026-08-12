package ounce.market.demo.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

    public ProductResponse(Long productId, String name, Long basePrice, long salePrice, long discountPercent, String description,
                           String imageUrl, int stock, ProductStatus status) {
        this.productId = productId;
        this.name = name;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.discountPercent = discountPercent;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.status = status;
    }


    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getProductId(), product.getName(), product.getBasePrice()
                , product.getSalePrice(), product.getDiscountPercent(), product.getDescription(),
                product.getImageUrl(),product.getStock(),product.getStatus());
    }

}
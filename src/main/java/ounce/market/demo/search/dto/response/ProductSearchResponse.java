package ounce.market.demo.search.dto.response;

import ounce.market.demo.product.entity.Product;
import ounce.market.demo.search.document.ProductDocument;

public record ProductSearchResponse(
        Long productId,
        String name,
        Long salePrice,
        Integer stock,
        String imageUrl
) {
    public static ProductSearchResponse from(ProductDocument d) {
        return new ProductSearchResponse(
                d.getProductId(), d.getName(), d.getSalePrice(), d.getStock(), null);
    }

    public static ProductSearchResponse from(Product p) {
        return new ProductSearchResponse(
                p.getProductId(), p.getName(), p.getSalePrice(), p.getStock(), p.getImageUrl());
    }
}
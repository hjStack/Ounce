package ounce.market.demo.product.exception;

import ounce.market.demo.product.entity.ProductStatus;

public class ProductNotPurchasableException extends RuntimeException {

    public ProductNotPurchasableException(Long productId, ProductStatus status) {
        super("현재 구매할 수 없는 상품 상태입니다. productId=" + productId + ", status=" + status);
    }
}

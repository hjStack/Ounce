package ounce.market.demo.product.exception;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(Long productId) {
        super("재고가 부족합니다. productId=" + productId);
    }
}

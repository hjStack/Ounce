package ounce.market.demo.member.exception;

public class CartNotFoundException extends RuntimeException{

    public CartNotFoundException(String message) {
        super(message);
    }
}

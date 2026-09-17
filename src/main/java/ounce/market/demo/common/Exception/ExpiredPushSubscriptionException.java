package ounce.market.demo.common.Exception;

public class ExpiredPushSubscriptionException extends RuntimeException{

    public ExpiredPushSubscriptionException(String message) {
        super(message);
    }
}

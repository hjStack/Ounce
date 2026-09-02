package ounce.market.demo.subscription.Exception;

import lombok.Getter;
import ounce.market.demo.subscription.entity.SubscriptionErrorCode;

@Getter
public class SubscriptionException extends RuntimeException {

    private final SubscriptionErrorCode errorCode;

    public SubscriptionException(SubscriptionErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}


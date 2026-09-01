package ounce.market.demo.coupon.error;

import org.springframework.http.HttpStatusCode;

public interface ErrorCode{
    String getCode();
    String getMessage();
    HttpStatusCode getStatus();


}
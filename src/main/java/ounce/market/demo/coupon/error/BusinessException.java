package ounce.market.demo.coupon.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    // RuntimeException을 상속해야 @Transactional이 롤백돼
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 금액 등 동적 값을 메시지에 넣어야 할 때 */
    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
}
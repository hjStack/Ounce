package ounce.market.demo.common.dto;


import ounce.market.demo.coupon.error.ErrorCode;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(), message);
    }

    // 기존 생성자 호출부 호환용
    public ErrorResponse(int status, String message) {
        this(status, null, message);
    }
}
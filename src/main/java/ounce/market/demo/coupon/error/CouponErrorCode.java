package ounce.market.demo.coupon.error;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CouponErrorCode implements ErrorCode {

    COUPON_NOT_FOUND("COUPON_001", "쿠폰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    POLICY_NOT_FOUND("COUPON_002", "쿠폰 정책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    COUPON_ALREADY_USED("COUPON_101", "이미 사용된 쿠폰입니다.", HttpStatus.CONFLICT),
    COUPON_EXPIRED("COUPON_102", "만료된 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    MIN_ORDER_AMOUNT_NOT_MET("COUPON_103", "최소 주문금액을 충족하지 않습니다.", HttpStatus.BAD_REQUEST),
    COUPON_ALREADY_APPLIED_TO_ORDER("COUPON_104", "해당 주문에 이미 쿠폰이 적용되어 있습니다.", HttpStatus.CONFLICT),
    CANNOT_EXPIRE_USED_COUPON("COUPON_105", "이미 사용된 쿠폰은 만료 처리할 수 없습니다.", HttpStatus.CONFLICT),

    COUPON_ISSUE_CLOSED("COUPON_201", "쿠폰 발급 기간이 아닙니다.", HttpStatus.BAD_REQUEST),
    COUPON_SOLD_OUT("COUPON_202", "쿠폰이 모두 소진되었습니다.", HttpStatus.CONFLICT),
    COUPON_ISSUE_LIMIT_EXCEEDED("COUPON_203", "이미 발급받은 쿠폰입니다.", HttpStatus.CONFLICT),

    INVALID_COUPON_POLICY("COUPON_901", "쿠폰 정책 설정이 올바르지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
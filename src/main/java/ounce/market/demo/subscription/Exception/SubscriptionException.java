package ounce.market.demo.subscription.Exception;

import lombok.Getter;

/**
 * 구독 도메인 예외.
 * <p>
 * 사용자에게 보여줄 문구는 errorCode.getMessage()에서만 나온다.
 * detail은 로그 전용이며 응답에 절대 싣지 않는다 — "구독 12의 nextBillingDate는 2026-09-07"
 * 같은 내부 상태가 응답에 실리면 그대로 정보 노출이다.
 */
@Getter
public class SubscriptionException extends RuntimeException {

    private final SubscriptionErrorCode errorCode;
    private final String detail;

    public SubscriptionException(SubscriptionErrorCode errorCode) {
        this(errorCode, null);
    }

    public SubscriptionException(SubscriptionErrorCode errorCode, String detail) {
        // 스택트레이스를 살려둔다. 도메인 예외라도 배치에서 원인을 못 찾으면 디버깅이 불가능하다.
        super(detail == null ? errorCode.getMessage() : errorCode.getMessage() + " (" + detail + ")");
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
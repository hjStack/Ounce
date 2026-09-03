package ounce.market.demo.subscription.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;

@Slf4j
@RestControllerAdvice
public class SubscriptionExceptionHandler {

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ErrorResponse> handle(SubscriptionException e) {
        SubscriptionErrorCode code = e.getErrorCode();

        // 4xx는 사용자 실수라 스택트레이스가 필요 없다. 5xx만 전체를 남긴다.
        if (code.getStatus().is5xxServerError()) {
            log.error("구독 처리 실패 code={} detail={}", code.getCode(), e.getDetail(), e);
        } else {
            log.info("구독 요청 거절 code={} detail={}", code.getCode(), e.getDetail());
        }

        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getCode(), code.getMessage()));
    }

    /**
     * 낙관적 락 충돌. @Version이 붙은 구독을 사용자와 결제 배치가 동시에 건드리면 여기로 온다.
     * 서버 오류가 아니라 재시도하면 되는 상황이므로 409로 내려 클라이언트가 안내하게 한다.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handle(ObjectOptimisticLockingFailureException e) {
        SubscriptionErrorCode code = SubscriptionErrorCode.CONCURRENT_MODIFICATION;
        log.warn("구독 동시 수정 충돌", e);
        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getCode(), code.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
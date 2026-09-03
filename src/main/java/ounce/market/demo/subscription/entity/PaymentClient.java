package ounce.market.demo.subscription.entity;

import org.springframework.stereotype.Component;

/**
 * 정기결제 PG 포트. 구현은 인프라 레이어(토스/나이스 등)에 둔다.
 * 구독 도메인은 이 인터페이스만 안다.
 */

// todo 실제로는 가입 시 카드를 등록하고 빌링키를 받아 저장하는 단계가 포함되어야 함

@Component
public interface PaymentClient {

    /**
     * @param idempotencyKey 회차 + 시도 번호로 만든 키. 같은 키로 두 번 요청해도 한 번만 청구되어야 한다.
     *                       네트워크 타임아웃 후 재시도할 때 이중 결제를 막는 유일한 수단이다.
     */
    PaymentResult charge(String idempotencyKey, Long memberId, long amount, String orderName);

    record PaymentResult(boolean success, String paymentKey, String failureCode, boolean retryable) {

        public static PaymentResult success(String paymentKey) {
            return new PaymentResult(true, paymentKey, null, false);
        }

        /** 한도 초과, 일시적 승인 거절 등 다시 시도해볼 만한 실패. */
        public static PaymentResult retryableFailure(String failureCode) {
            return new PaymentResult(false, null, failureCode, true);
        }

        /** 카드 정지, 유효기간 만료 등 재시도해도 결과가 같은 실패. */
        public static PaymentResult permanentFailure(String failureCode) {
            return new PaymentResult(false, null, failureCode, false);
        }
    }
}
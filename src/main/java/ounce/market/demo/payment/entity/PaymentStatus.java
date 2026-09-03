package ounce.market.demo.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PENDING("결제 대기"),          // 아직 PG에 요청이 나가지 않았다. 돈이 안 빠져나간 게 확실한 상태.
    COMPLETED("결제 완료"),        // 승인됨. (구독은 이 시점에 배송 확정)
    FAILED("결제 실패"),           // 잔액 부족, 한도 초과 등으로 승인 거절됨
    CANCELED("결제 취소"),         // 사용자가 결제창에서 이탈. 주문 결제에만 해당한다.
    REFUNDED("환불 완료"),         // 결제 완료 후 환불

    /**
     * 결과 미확인. PG에 요청은 나갔는데 응답을 못 받았다.
     * <p>
     * PENDING과 반드시 구분해야 한다. PENDING은 "돈이 안 나간 게 확실"이고
     * 이쪽은 "나갔을 수도 있음"이다. 둘을 같은 값으로 두면 타임아웃 난 건을
     * 미결제로 착각해 재청구하게 되고, 그게 그대로 이중 결제다.
     * 대사 배치가 PG 조회 API로 진위를 확인할 때까지 이 상태로 둔다.
     */
    UNKNOWN("결제 결과 확인 중");

    private final String description;

    /** PG 응답이 확정된 상태인가. PENDING만 아직 요청 전이다. */
    public boolean isResolved() {
        return this != PENDING;
    }

    /** 대사 배치가 확인해야 하는 상태인가. */
    public boolean needsReconciliation() {
        return this == UNKNOWN;
    }
}
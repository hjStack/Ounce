package ounce.market.demo.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.subscription.entity.SubscriptionCycle;

import java.time.LocalDateTime;

/**
 * 결제 시도 1회. 한 회차에 최대 3행이 쌓인다.
 * <p>
 * 회차(SubscriptionCycle)가 "이번 주에 무엇을 얼마에 배송하기로 했나"라면,
 * 이쪽은 "그 돈을 받아내려고 몇 시에 몇 번 시도했고 PG가 뭐라고 답했나"이다.
 * 카드사 대사와 CS 문의 대응이 전부 이 테이블에서 나온다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "payment_attempt",
        uniqueConstraints = {
                // 같은 멱등키로 두 행이 생기면 이중 결제를 의심해야 한다. DB에서 막는다.
                @UniqueConstraint(name = "uk_attempt_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_attempt_cycle", columnList = "cycle_id"),
                // 응답을 못 받은 시도를 훑는 대사 배치 경로
                @Index(name = "idx_attempt_status_requested", columnList = "status, requested_at")
        }
)
public class PaymentAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private SubscriptionCycle cycle;

    /** 회차 안에서 몇 번째 시도인지. 1부터 시작해 최대 3. */
    @Column(nullable = false)
    private int attemptNo;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private long amount;

    /** PG를 호출하기 직전 시각. 이 행은 호출 전에 커밋된다. */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

    private String paymentKey;

    private String failureCode;

    private PaymentAttempt(SubscriptionCycle cycle, int attemptNo, String idempotencyKey,
                           long amount, LocalDateTime requestedAt) {
        this.cycle = cycle;
        this.attemptNo = attemptNo;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.requestedAt = requestedAt;
        this.status = PaymentStatus.PENDING;
    }

    public static PaymentAttempt request(SubscriptionCycle cycle, int attemptNo, String idempotencyKey,
                                         long amount, LocalDateTime requestedAt) {
        return new PaymentAttempt(cycle, attemptNo, idempotencyKey, amount, requestedAt);
    }

    /**
     * PG 응답이 확정된 시도인가. 결제 결과를 두 번 반영하는 것을 막는 가드로 쓴다.
     * UNKNOWN도 확정으로 친다 — 대사 배치가 따로 처리할 몫이지, 결제 배치가 덮어쓸 대상이 아니다.
     */
    public boolean isResolved() {
        return this.status.isResolved();
    }

    public void markSucceeded(String paymentKey, LocalDateTime respondedAt) {
        this.status = PaymentStatus.COMPLETED;
        this.paymentKey = paymentKey;
        this.respondedAt = respondedAt;
        this.failureCode = null;
    }

    public void markFailed(String failureCode, LocalDateTime respondedAt) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        this.respondedAt = respondedAt;
    }

    /**
     * PG 응답을 못 받았다. 실패가 아니라 "결과 모름"이다.
     * 실제로 승인됐을 수 있으므로 대사 배치가 PG 조회 API로 확인할 때까지 이 상태로 둔다.
     * PENDING으로 되돌리면 안 된다 — 그건 "요청 전"이라는 뜻이라 재청구 대상이 된다.
     */
    public void markUnknown(String reason, LocalDateTime respondedAt) {
        this.status = PaymentStatus.UNKNOWN;
        this.failureCode = reason;
        this.respondedAt = respondedAt;
    }

    /** 대사 결과 실제로는 승인돼 있던 경우. */
    public void reconcileAsSucceeded(String paymentKey, LocalDateTime reconciledAt) {
        this.status = PaymentStatus.COMPLETED;
        this.paymentKey = paymentKey;
        this.respondedAt = reconciledAt;
        this.failureCode = null;
    }

    /** 대사 결과 승인된 적이 없던 경우. */
    public void reconcileAsFailed(String failureCode, LocalDateTime reconciledAt) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        this.respondedAt = reconciledAt;
    }
}
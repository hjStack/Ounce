package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.payment.entity.BillingContext;
import ounce.market.demo.payment.entity.PaymentAttempt;
import ounce.market.demo.payment.repository.PaymentAttemptRepository;
import ounce.market.demo.subscription.entity.*;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.event.SubscriptionPaymentSucceededEvent;
import ounce.market.demo.subscription.repository.SubscriptionCycleRepository;
import ounce.market.demo.subscription.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 구독 1건에 대한 결제 트랜잭션 단위.
 * <p>
 * 메서드가 셋으로 쪼개져 있는 게 핵심이다. PG 호출을 트랜잭션 안에 넣으면
 * 커밋 실패 시 "카드는 긁혔는데 회차는 없는" 상태가 만들어진다. 그래서
 * (1) 회차와 시도 이력을 커밋 → (2) 트랜잭션 밖에서 PG 호출 → (3) 결과를 새 트랜잭션에 반영
 * 순서로 진행한다. 2번에서 서버가 죽어도 REQUESTED 시도가 남아 대사가 가능하다.
 * <p>
 * 각 메서드가 REQUIRES_NEW인 이유는 배치 한 건의 실패가 나머지 구독을 롤백시키면 안 되기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionBillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCycleRepository cycleRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 1단계. 메뉴 선택 중이던 회차를 확정하고 첫 시도 이력을 커밋한다.
     * <p>
     * 회차를 여기서 만들지 않는다. 직전 결제 직후 열린 DRAFT 회차에서 사용자가
     * 한 주 내내 메뉴를 골라왔다. 여기서는 메뉴를 닫고 금액과 배송일을 고정할 뿐이다.
     * 스캔 시점 이후 사용자가 쉬어가기를 눌렀을 수 있으므로 상태는 다시 확인한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<BillingContext> prepare(Long subscriptionId, LocalDateTime now) {
        Subscription subscription = loadSubscription(subscriptionId);

        if (!subscription.isBillableOn(now.toLocalDate())) {
            return Optional.empty();
        }

        Optional<SubscriptionCycle> draft = cycleRepository.findDraft(subscriptionId);
        if (draft.isEmpty()) {
            // 메뉴 오픈 배치가 돌지 않았다. 결제를 강행하면 빈 박스를 배송하게 된다.
            log.error("DRAFT 회차 없이 결제일이 됐다 subscriptionId={}", subscriptionId);
            return Optional.empty();
        }

        SubscriptionCycle cycle = draft.get();
        cycle.confirmForBilling(now);

        return Optional.of(openAttempt(subscription, cycle, now));
    }

    /**
     * 1단계(재시도판). 회차를 새로 만들지 않고 기존 회차에 시도 이력만 덧붙인다.
     * 재시도마다 회차를 새로 만들면 회차 번호가 튀고 쿠폰 카운트가 오염된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<BillingContext> prepareRetry(Long subscriptionId, LocalDateTime now) {
        Subscription subscription = loadSubscription(subscriptionId);

        if (!subscription.isRetryDue(now)) {
            return Optional.empty();
        }

        return Optional.of(openAttempt(subscription, loadCurrentCycle(subscription), now));
    }

    /**
     * 3단계(성공). 구독 상태를 넘기고 쿠폰 조건을 판정한다.
     * <p>
     * 멱등이어야 한다. 대사 배치가 UNKNOWN 시도를 승인으로 확정하는 것과
     * 늦게 도착한 PG 응답을 결제 배치가 반영하는 것이 겹칠 수 있다.
     * 가드가 없으면 onPaymentSucceeded()가 두 번 돌아 다음 결제일이 2주 밀리고
     * 쿠폰 카운트가 2씩 오른다. 돈이 얽힌 경로라 되돌리기도 어렵다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applySuccess(BillingContext context, String paymentKey, LocalDateTime now) {
        Subscription subscription = loadSubscription(context.subscriptionId());
        SubscriptionCycle cycle = loadCycle(context.cycleId());
        PaymentAttempt attempt = loadAttempt(context.attemptId());

        // 회차가 이미 확정됐으면 구독 상태는 손대지 않는다.
        // 이 회차의 결제는 어느 경로로든 이미 반영이 끝났다는 뜻이다.
        if (cycle.isPaid()) {
            if (!attempt.isResolved()) {
                attempt.markSucceeded(paymentKey, now);
            }
            return;
        }

        attempt.markSucceeded(paymentKey, now);

        // 재시도로 날짜가 넘어갔으면 배송일도 다시 계산해야 한다.
        // 월 23시 전 성공이면 화요일, 자정 넘겨 성공하면 수요일 새벽이다.
        cycle.markPaid(paymentKey, Subscription.deliveryDateOf(now));

        int streak = subscription.onPaymentSucceeded();

        // 쿠폰 발급은 이 트랜잭션이 커밋된 뒤로 미룬다.
        // 여기서 직접 부르면 쿠폰 모듈의 실패가 결제를 롤백시킨다.
        eventPublisher.publishEvent(new SubscriptionPaymentSucceededEvent(
                context.memberId(), context.subscriptionId(), context.cycleId(),
                streak, subscription.isCouponIssuable()));
    }

    /**
     * 3단계(실패). 재시도를 잡을지, 이번 주를 포기할지, 해지할지는 엔티티가 판단한다.
     * 재시도 가치가 없는 실패(카드 정지 등)는 남은 시도를 소진시키지 않고 곧장 이번 주를 넘긴다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentFailureResult applyFailure(BillingContext context, String failureCode,
                                             boolean retryable, LocalDateTime now) {
        Subscription subscription = loadSubscription(context.subscriptionId());
        SubscriptionCycle cycle = loadCycle(context.cycleId());
        PaymentAttempt attempt = loadAttempt(context.attemptId());

        // 이미 결과가 반영된 시도다. 실패를 두 번 세면 실패 주 수가 부풀어
        // 멀쩡한 구독이 3주 규칙에 걸려 해지된다.
        if (attempt.isResolved()) {
            return currentFailureResult(subscription);
        }

        attempt.markFailed(failureCode, now);

        PaymentFailureResult result = retryable
                ? subscription.onPaymentFailed(now)
                : subscription.abandonCycle(now, failureCode);

        if (result.isCycleAbandoned()) {
            cycle.markAbandoned(failureCode);
        } else {
            cycle.markFailed(failureCode);
        }
        return result;
    }

    /**
     * 3단계(응답 없음). 승인 여부를 모르므로 구독 상태는 건드리지 않는다.
     * 여기서 실패로 확정하면, 실제로는 승인된 건에 대해 2시간 뒤 또 청구하게 된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUnknown(BillingContext context, String reason, LocalDateTime now) {
        PaymentAttempt attempt = loadAttempt(context.attemptId());
        if (!attempt.isResolved()) {
            attempt.markUnknown(reason, now);
        }
    }

    /**
     * 이미 반영이 끝난 시도에 대해 현재 구독 상태로부터 결말을 되읽는다.
     * 상태를 바꾸지 않고 호출자에게 같은 답을 돌려주기 위한 것이다.
     */
    private PaymentFailureResult currentFailureResult(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case PAYMENT_RETRYING -> PaymentFailureResult.RETRY_SCHEDULED;
            case CANCELED -> PaymentFailureResult.SUBSCRIPTION_CANCELED;
            default -> PaymentFailureResult.WEEK_SKIPPED;
        };
    }

    /** 재시도 마감이 지났는데 남아 있는 구독 정리. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentFailureResult expireRetry(Long subscriptionId, LocalDateTime now) {
        Subscription subscription = loadSubscription(subscriptionId);
        loadCurrentCycle(subscription).markAbandoned("RETRY_DEADLINE_EXPIRED");
        return subscription.expireRetry(now);
    }

    /** 쉬어가기 종료일 도래분 재개. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resumePaused(Long subscriptionId, LocalDateTime now) {
        Subscription subscription = loadSubscription(subscriptionId);
        if (subscription.isResumeDue(now.toLocalDate())) {
            subscription.resume(now.toLocalDate());
        }
    }

    /**
     * 시도 이력을 만들고 컨텍스트로 옮긴다.
     * 멱등키에 시도 번호를 넣을지가 갈림길이다. 넣으면 재시도가 별건으로 나가고,
     * 빼면 PG가 첫 시도의 거절 결과를 그대로 돌려준다. 승인 거절 후 재시도가 목적이므로 포함한다.
     */
    private BillingContext openAttempt(Subscription subscription, SubscriptionCycle cycle, LocalDateTime now) {
        int attemptNo = cycle.beginAttempt();
        String idempotencyKey = "sub-%d-cycle-%d-try-%d".formatted(
                subscription.getSubscriptionId(), cycle.getCycleNumber(), attemptNo);

        PaymentAttempt attempt = attemptRepository.save(PaymentAttempt.request(
                cycle, attemptNo, idempotencyKey, cycle.getAmount(), now));

        return new BillingContext(
                subscription.getSubscriptionId(),
                subscription.getMember().getMemberId(),
                cycle.getCycleId(),
                attempt.getAttemptId(),
                cycle.getCycleNumber(),
                attemptNo,cycle.getAmount(),
                idempotencyKey
        );
    }

    private Subscription loadSubscription(Long id) {
        return subscriptionRepository.findWithMember(id)
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND, "subscriptionId=" + id));
    }

    private SubscriptionCycle loadCycle(Long cycleId) {
        return cycleRepository.findById(cycleId)
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.CYCLE_NOT_FOUND, "cycleId=" + cycleId));
    }

    private SubscriptionCycle loadCurrentCycle(Subscription subscription) {
        return cycleRepository
                .findBySubscription_SubscriptionIdAndCycleNumber(
                        subscription.getSubscriptionId(), subscription.getLastCycleNumber())
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.CYCLE_NOT_FOUND,
                        "subscriptionId=%d cycleNumber=%d".formatted(
                                subscription.getSubscriptionId(), subscription.getLastCycleNumber())));
    }

    private PaymentAttempt loadAttempt(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.PAYMENT_ATTEMPT_NOT_FOUND, "attemptId=" + attemptId));
    }
}
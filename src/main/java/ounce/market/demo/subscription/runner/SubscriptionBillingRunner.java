package ounce.market.demo.subscription.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import ounce.market.demo.payment.entity.BillingContext;
import ounce.market.demo.subscription.entity.PaymentClient;
import ounce.market.demo.subscription.entity.PaymentFailureResult;
import ounce.market.demo.subscription.repository.SubscriptionRepository;
import ounce.market.demo.subscription.service.SubscriptionBillingService;
import ounce.market.demo.subscription.service.SubscriptionMenuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 배치 오케스트레이션. 이 클래스에는 @Transactional이 없다.
 * 트랜잭션 경계는 SubscriptionBillingService 안에 있고, PG 호출은 그 밖에서 일어난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingRunner {

    private static final int CHUNK_SIZE = 200;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBillingService billingService;
    private final PaymentClient paymentClient;
    private final SubscriptionMenuService menuService;

    /**
     * 열려 있는 회차가 없는 구독의 다음 회차를 연다. 안전망이다.
     * 정상 경로에서는 결제 성공 직후 곧바로 열리므로 여기 걸리는 건
     * 쉬어가기나 결제 실패로 결제일이 밀린 구독, 또는 결제 직후 개설이 실패한 건이다.
     */
    public void runOpenMenu(LocalDateTime now) {
        forEachChunk(
                lastId -> subscriptionRepository.findIdsWithoutOpenCycle(lastId, PageRequest.of(0, CHUNK_SIZE)),
                menuService::openNextCycle
        );
    }

    /** 매일 23시. 오늘이 결제일인 ACTIVE 구독을 청구한다. */
    public void runBilling(LocalDateTime now) {
        forEachChunk(
                lastId -> subscriptionRepository.findIdsToBill(now.toLocalDate(), lastId, PageRequest.of(0, CHUNK_SIZE)),
                id -> charge(id, now, billingService::prepare)
        );
    }

    /** 재시도 슬롯 스윕. nextRetryAt이 도래한 건만 집는다. */
    public void runRetry(LocalDateTime now) {
        forEachChunk(
                lastId -> subscriptionRepository.findIdsToRetry(now, lastId, PageRequest.of(0, CHUNK_SIZE)),
                id -> charge(id, now, billingService::prepareRetry)
        );
    }

    /** 마감 지난 재시도 정리. */
    public void runExpireRetry(LocalDateTime now) {
        forEachChunk(
                lastId -> subscriptionRepository.findIdsWithExpiredRetry(now, lastId, PageRequest.of(0, CHUNK_SIZE)),
                id -> {
                    PaymentFailureResult result = billingService.expireRetry(id, now);
                    log.info("재시도 마감 정리 subscriptionId={} result={}", id, result);
                }
        );
    }

    /** 쉬어가기 종료일 도래분 재개. 결제 배치보다 먼저 돌아야 그날 결제가 잡힌다. */
    public void runResume(LocalDateTime now) {
        forEachChunk(
                lastId -> subscriptionRepository.findIdsToResume(now.toLocalDate(), lastId, PageRequest.of(0, CHUNK_SIZE)),
                id -> billingService.resumePaused(id, now)
        );
    }

    /**
     * 사용자가 메뉴를 확정하는 즉시 결제한다. 첫 회차 결제 경로다.
     * <p>
     * 배치와 같은 3단계(회차 확정 → PG 호출 → 결과 반영)를 그대로 탄다.
     * 화면이 결과를 바로 보여줘야 하므로 결말을 돌려준다.
     */
    public ChargeOutcome chargeNow(Long subscriptionId, LocalDateTime now) {
        return charge(subscriptionId, now, billingService::prepare);
    }

    private ChargeOutcome charge(Long subscriptionId, LocalDateTime now,
                                 PrepareStep prepareStep) {
        Optional<BillingContext> prepared = prepareStep.prepare(subscriptionId, now);
        if (prepared.isEmpty()) {
            // 스캔 이후 사용자가 쉬어가기를 눌렀거나 해지했다. 정상 흐름이다.
            return ChargeOutcome.NOT_BILLABLE;
        }
        BillingContext context = prepared.get();

        PaymentClient.PaymentResult result;
        try {
            // 트랜잭션 밖. 여기서 죽어도 회차는 PENDING으로 커밋되어 있다.
            result = paymentClient.charge(
                    context.idempotencyKey(), context.memberId(), context.amount(), context.orderName());
        } catch (Exception e) {
            // PG 응답을 못 받았다. 실제로 승인됐을 수도 있으므로 실패로 확정하지 않는다.
            // 시도를 UNKNOWN으로 남기고, 대사 배치가 PG 조회 API로 진위를 확인한다.
            billingService.markUnknown(context, "PG_NO_RESPONSE", now);
            log.error("PG 응답 없음. 대사 필요 subscriptionId={} key={}",
                    subscriptionId, context.idempotencyKey(), e);
            return ChargeOutcome.NO_RESPONSE;
        }

        if (result.success()) {
            billingService.applySuccess(context, result.paymentKey(), now);
            // 결제가 끝났으니 다음 회차 메뉴 창을 바로 연다.
            // 별도 트랜잭션이라 여기서 실패해도 결제는 남고, 안전망 배치가 다음 날 집는다.
            menuService.openNextCycle(subscriptionId);
            return ChargeOutcome.PAID;
        }

        PaymentFailureResult failure = billingService.applyFailure(
                context, result.failureCode(), result.retryable(), now);
        log.info("결제 실패 subscriptionId={} attempt={} code={} result={}",
                subscriptionId, context.attemptId(), result.failureCode(), failure);
        // 여기서 알림을 쏜다. WEEK_SKIPPED면 "이번 주는 쉬어갑니다",
        // SUBSCRIPTION_CANCELED면 해지 안내. 이벤트 발행으로 빼는 편이 낫다.
        return ChargeOutcome.FAILED;
    }

    /** 결제 한 건의 결말. 즉시 결제 화면이 이 값으로 분기한다. */
    public enum ChargeOutcome {
        PAID,
        FAILED,
        /** 결제일이 아니거나 이미 쉬어가기·해지된 구독이다. */
        NOT_BILLABLE,
        /** PG 응답을 못 받았다. 승인 여부 불명이라 사용자에게 재시도를 권하면 안 된다. */
        NO_RESPONSE
    }

    /**
     * ID 키셋으로 청크를 돌린다. 건별 예외는 삼키고 다음 건으로 넘어간다 —
     * 한 사용자의 카드 문제로 나머지 구독의 결제가 멈추면 안 된다.
     */
    private void forEachChunk(Function<Long, List<Long>> fetcher, ItemHandler handler) {
        Long lastId = 0L;
        while (true) {
            List<Long> ids = fetcher.apply(lastId);
            if (ids.isEmpty()) {
                return;
            }
            for (Long id : ids) {
                try {
                    handler.handle(id);
                } catch (ObjectOptimisticLockingFailureException e) {
                    // 처리 도중 사용자가 같은 구독을 건드렸다. 다음 스윕에서 다시 집힌다.
                    log.warn("동시 수정 충돌 subscriptionId={}", id);
                } catch (Exception e) {
                    log.error("구독 처리 실패 subscriptionId={}", id, e);
                }
            }
            lastId = ids.get(ids.size() - 1);
        }
    }

    @FunctionalInterface
    private interface PrepareStep {
        Optional<BillingContext> prepare(Long subscriptionId, LocalDateTime now);
    }

    @FunctionalInterface
    private interface ItemHandler {
        void handle(Long subscriptionId);
    }
}
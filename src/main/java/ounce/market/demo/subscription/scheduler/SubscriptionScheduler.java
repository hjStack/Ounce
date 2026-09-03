package ounce.market.demo.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ounce.market.demo.subscription.runner.SubscriptionBillingRunner;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 서버를 2대 이상 띄우면 같은 크론이 두 번 돈다. 결제 배치에서 이건 곧 이중 결제다.
 * 멱등키가 1차 방어선이지만, ShedLock 같은 분산 락을 반드시 걸어야 한다.
 * (@SchedulerLock 어노테이션은 shedlock-spring 의존성 추가 후 각 메서드에 붙인다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionBillingRunner runner;
    private final Clock clock;

    /** 22:50 — 결제 직전에 쉬어가기 종료분을 먼저 ACTIVE로 되돌린다. */
    @Scheduled(cron = "0 50 22 * * *", zone = "Asia/Seoul")
    public void resumePaused() {
        runner.runResume(LocalDateTime.now(clock));
    }

    /**
     * 23:00 — 주문 마감과 동시에 결제한다.
     * 이 시각에 성공하면 다음날 새벽 배송, 재시도로 자정을 넘기면 그 다음날 새벽 배송이 된다.
     */
    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    public void billToday() {
        runner.runBilling(LocalDateTime.now(clock));
    }

    /**
     * 10분마다 재시도 대상을 훑는다.
     * "2시간마다"를 크론으로 잡으면 안 된다. 재시도 시각은 구독마다 실패 시각 + 2시간이라
     * 전부 제각각이다. 간격은 엔티티의 nextRetryAt이 정하고, 스케줄러는 도래분만 집는다.
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void retryFailed() {
        runner.runRetry(LocalDateTime.now(clock));
    }

    /** 매시 정각 — 재시도 마감이 지났는데 상태가 남아 있는 구독을 정리한다. */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void expireRetries() {
        runner.runExpireRetry(LocalDateTime.now(clock));
    }
}
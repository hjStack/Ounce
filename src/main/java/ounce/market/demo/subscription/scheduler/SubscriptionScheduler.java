package ounce.market.demo.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ounce.market.demo.subscription.service.CycleGenerationService;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final CycleGenerationService cycleGenerationService;

    /**
     * 매일 새벽 3시. 결제일이 다가온 구독의 회차를 만든다.
     * 매일 도는 이유는, 구독 시작일이 회원마다 달라 결제 요일이 제각각이기 때문이다.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void generateCycles() {
        log.info("회차 생성 배치 시작");
        cycleGenerationService.generateCycles(LocalDate.now());
    }
}
package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionCycle;
import ounce.market.demo.subscription.entity.SubscriptionStatus;
import ounce.market.demo.subscription.repository.SubscriptionCycleRepository;
import ounce.market.demo.subscription.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 매주 회차를 만들어내는 서비스.
 * 배치가 이 클래스를 호출하고, 트랜잭션은 구독 하나 단위로 끊는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleGenerationService {

    /**
     * 배송일보다 며칠 앞서 회차를 만들어둘지.
     * 사용자가 메뉴를 고를 시간을 주려면 마감(배송 2일 전)보다 충분히 앞서야 한다.
     */
    public static final int GENERATE_DAYS_AHEAD = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCycleRepository cycleRepository;

    /**
     * 결제일이 도래한 구독들의 다음 회차를 만든다.
     * 배치에서 호출하는 진입점이며, 구독 하나가 실패해도 나머지는 계속 처리한다.
     */
    public CycleGenerationResult generateCycles(LocalDate today) {
        LocalDate targetBillingDate = today.plusDays(GENERATE_DAYS_AHEAD);

        List<Subscription> targets = subscriptionRepository
                .findAllByStatusAndNextBillingDateLessThanEqual(
                        SubscriptionStatus.ACTIVE, targetBillingDate);

        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (Subscription subscription : targets) {
            try {
                boolean generated = generateOneCycle(subscription.getSubscriptionId());
                if (generated) {
                    created++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                // 한 건의 실패가 전체 배치를 멈추면 안 된다.
                failed++;
                log.error("회차 생성 실패. subscriptionId={}",
                        subscription.getSubscriptionId(), e);
            }
        }

        log.info("회차 생성 배치 완료. 대상={}, 생성={}, 건너뜀={}, 실패={}",
                targets.size(), created, skipped, failed);

        return new CycleGenerationResult(targets.size(), created, skipped, failed);
    }

    /**
     * 구독 하나의 다음 회차를 만든다.
     * 트랜잭션을 여기서 새로 열어, 한 건이 롤백돼도 다른 건에 영향이 없게 한다.
     *
     * @return 새로 만들었으면 true, 이미 있어서 건너뛰었으면 false
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateOneCycle(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalStateException(
                        "구독이 사라졌습니다. subscriptionId=" + subscriptionId));

        // 조회 시점과 처리 시점 사이에 해지됐을 수 있다.
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return false;
        }

        int nextNumber = subscription.getLastCycleNumber() + 1;

        // 유니크 제약이 최종 방어선이지만, 예외를 만들지 않는 게 로그가 깨끗하다.
        if (cycleRepository.existsBySubscriptionSubscriptionIdAndCycleNumber(
                subscriptionId, nextNumber)) {
            return false;
        }

        SubscriptionCycle cycle = SubscriptionCycle.builder()
                .subscription(subscription)
                .cycleNumber(subscription.nextCycleNumber())
                .mealsCount(subscription.getMealsPerWeek())
                .deliveryDate(subscription.getNextBillingDate())
                .build();

        try {
            cycleRepository.save(cycle);
        } catch (DataIntegrityViolationException e) {
            // 배치가 동시에 두 번 돌면 여기로 온다. 정상 동작이므로 조용히 넘긴다.
            log.debug("회차가 이미 존재합니다. subscriptionId={}, cycleNumber={}",
                    subscriptionId, nextNumber);
            return false;
        }
        return true;
    }

    public record CycleGenerationResult(int total, int created, int skipped, int failed) {
    }
}
package ounce.market.demo.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.subscription.entity.SubscriptionCycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionCycleRepository extends JpaRepository<SubscriptionCycle, Long> {

    Optional<SubscriptionCycle> findBySubscription_SubscriptionIdAndCycleNumber(Long subscriptionId, int cycleNumber);

    /**
     * 메뉴 선택 중인 회차. 구독당 1건이어야 한다.
     * <p>
     * 유니크 제약이 이중 생성을 막지만, 그래도 최신 것 하나만 집도록 정렬과 limit을 둔다.
     * 데이터가 깨졌을 때 NonUniqueResultException으로 조회 전체가 죽는 것보다,
     * 하나를 돌려주고 서비스가 계속 도는 편이 낫다.
     */
    @Query("""
            select c from SubscriptionCycle c
            left join fetch c.items
            where c.subscription.subscriptionId = :subscriptionId
              and c.status = ounce.market.demo.subscription.entity.SubscriptionCycleStatus.DRAFT
            order by c.cycleNumber desc
            limit 1
            """)
    Optional<SubscriptionCycle> findDraft(@Param("subscriptionId") Long subscriptionId);

    boolean existsBySubscription_SubscriptionIdAndCycleNumber(Long subscriptionId, int cycleNumber);

    /** 직전 회차. 메뉴를 안 고른 사용자에게 기본 구성을 채울 때 참고한다. */
    @Query("""
            select c from SubscriptionCycle c
            left join fetch c.items
            where c.subscription.subscriptionId = :subscriptionId
              and c.status <> ounce.market.demo.subscription.entity.SubscriptionCycleStatus.DRAFT
            order by c.cycleNumber desc
            limit 1
            """)
    Optional<SubscriptionCycle> findLatestConfirmed(@Param("subscriptionId") Long subscriptionId);

    /** 배송 배치가 집어갈 목록. PAID인 회차만 나간다. */
    @Query("""
            select c from SubscriptionCycle c
            join fetch c.subscription s
            join fetch s.member
            where c.deliveryDate = :deliveryDate
              and c.status = ounce.market.demo.subscription.entity.SubscriptionCycleStatus.PAID
            """)
    List<SubscriptionCycle> findDeliverableOn(@Param("deliveryDate") LocalDate deliveryDate);

    @Query("""
            select c from SubscriptionCycle c
            where c.subscription.subscriptionId = :subscriptionId
            order by c.cycleNumber desc
            """)
    List<SubscriptionCycle> findHistory(@Param("subscriptionId") Long subscriptionId);
}
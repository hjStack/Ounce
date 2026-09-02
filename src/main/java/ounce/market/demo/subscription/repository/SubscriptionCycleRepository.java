package ounce.market.demo.subscription.repository;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.subscription.entity.CycleStatus;
import ounce.market.demo.subscription.entity.SubscriptionCycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionCycleRepository extends JpaRepository<SubscriptionCycle, Long> {

    /** 배치 중복 실행 방어. 유니크 제약과 별개로 미리 걸러내는 용도. */
    boolean existsBySubscriptionSubscriptionIdAndCycleNumber(Long subscriptionId, int cycleNumber);

    List<SubscriptionCycle> findAllBySubscriptionSubscriptionIdOrderByCycleNumberDesc(
            Long subscriptionId);

    Optional<SubscriptionCycle> findBySubscriptionSubscriptionIdAndCycleNumber(
            Long subscriptionId, int cycleNumber);

    /**
     * 마감 배치용. 배송일이 특정일인 미확정 회차를 훑는다.
     * idx_cycle_status_delivery(status, delivery_date)를 탄다.
     */
    List<SubscriptionCycle> findAllByStatusAndDeliveryDate(
            CycleStatus status, LocalDate deliveryDate);

    /** 결제 배치용. 구독과 회원까지 함께 로딩해서 N+1을 피한다. */
    @EntityGraph(attributePaths = {"subscription", "subscription.member"})
    List<SubscriptionCycle> findAllByStatusAndDeliveryDateLessThanEqual(
            CycleStatus status, LocalDate deliveryDate);
}
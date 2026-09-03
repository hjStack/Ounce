package ounce.market.demo.subscription.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.subscription.entity.Subscription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("select s from Subscription s join fetch s.member where s.subscriptionId = :id")
    Optional<Subscription> findWithMember(@Param("id") Long id);

    /**
     * 오늘 결제할 구독 ID.
     * 엔티티가 아니라 ID만 뽑는 이유는, 배치가 건당 별도 트랜잭션으로 다시 조회하기 때문이다.
     * 스캔 시점과 처리 시점 사이에 사용자가 쉬어가기를 누를 수 있으므로 상태는 처리 시점에 다시 본다.
     * offset 페이징은 처리 중 상태가 바뀌면 행을 건너뛰므로 ID 키셋으로 넘긴다.
     */
    @Query("""
            select s.subscriptionId from Subscription s
            where s.status = ounce.market.demo.subscription.entity.SubscriptionStatus.ACTIVE
              and s.nextBillingDate <= :today
              and s.subscriptionId > :lastId
            order by s.subscriptionId asc
            """)
    List<Long> findIdsToBill(@Param("today") LocalDate today,
                             @Param("lastId") Long lastId,
                             Pageable pageable);

    /** 재시도 시각이 도래했고 아직 마감 전인 구독. */
    @Query("""
            select s.subscriptionId from Subscription s
            where s.status = ounce.market.demo.subscription.entity.SubscriptionStatus.PAYMENT_RETRYING
              and s.nextRetryAt <= :now
              and s.retryDeadline >= :now
              and s.subscriptionId > :lastId
            order by s.subscriptionId asc
            """)
    List<Long> findIdsToRetry(@Param("now") LocalDateTime now,
                              @Param("lastId") Long lastId,
                              Pageable pageable);

    /** 재시도 마감이 지났는데 PAYMENT_RETRYING으로 남아 있는 구독. 정리 대상. */
    @Query("""
            select s.subscriptionId from Subscription s
            where s.status = ounce.market.demo.subscription.entity.SubscriptionStatus.PAYMENT_RETRYING
              and s.retryDeadline < :now
              and s.subscriptionId > :lastId
            order by s.subscriptionId asc
            """)
    List<Long> findIdsWithExpiredRetry(@Param("now") LocalDateTime now,
                                       @Param("lastId") Long lastId,
                                       Pageable pageable);

    /** 쉬어가기 종료일이 도래한 구독. */
    @Query("""
            select s.subscriptionId from Subscription s
            where s.status = ounce.market.demo.subscription.entity.SubscriptionStatus.PAUSED
              and s.resumeDate <= :today
              and s.subscriptionId > :lastId
            order by s.subscriptionId asc
            """)
    List<Long> findIdsToResume(@Param("today") LocalDate today,
                               @Param("lastId") Long lastId,
                               Pageable pageable);

    @Query("""
            select count(s) > 0 from Subscription s
            where s.member.memberId = :memberId
              and s.status <> ounce.market.demo.subscription.entity.SubscriptionStatus.CANCELED
            """)
    boolean existsActiveByMember(@Param("memberId") Long memberId);

    @Query("""
            select s from Subscription s
            where s.member.memberId = :memberId
            order by s.subscriptionId desc
            """)
    List<Subscription> findAllByMember(@Param("memberId") Long memberId);

    @Query("""
        select s.subscriptionId from Subscription s
        where s.status = ounce.market.demo.subscription.entity.SubscriptionStatus.ACTIVE
          and s.nextBillingDate is not null
          and not exists (
                select 1 from SubscriptionCycle c
                where c.subscription = s
                  and c.status = ounce.market.demo.subscription.entity.SubscriptionCycleStatus.DRAFT)
          and s.subscriptionId > :lastId
        order by s.subscriptionId asc
        """)
    List<Long> findIdsWithoutOpenCycle(@Param("lastId") Long lastId, Pageable pageable);
}
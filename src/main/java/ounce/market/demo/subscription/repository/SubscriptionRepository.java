package ounce.market.demo.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** 회원의 현재 유효한 구독. 해지된 건 제외한다. */
    Optional<Subscription> findByMemberMemberIdAndStatusIn(
            Long memberId, Collection<SubscriptionStatus> statuses);

    boolean existsByMemberMemberIdAndStatusIn(
            Long memberId, Collection<SubscriptionStatus> statuses);

    /** 해지 이력까지 포함한 전체 목록 */
    List<Subscription> findAllByMemberMemberIdOrderBySubscriptionIdDesc(Long memberId);

    /**
     * 배치용. 결제일이 도래한 활성 구독을 훑는다.
     * idx_subscription_billing(status, next_billing_date)를 탄다.
     */
    List<Subscription> findAllByStatusAndNextBillingDateLessThanEqual(
            SubscriptionStatus status, LocalDate date);
}
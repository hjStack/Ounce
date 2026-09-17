package ounce.market.demo.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.notification.entity.PushSubscription;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByMemberMidnightAlertEnabledTrue();
    void deleteByEndpoint(String endpoint);

    List<PushSubscription> findAll();
    List<PushSubscription> findAllByMidnightAlertEnabledTrue();
}

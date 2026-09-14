package ounce.market.demo.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMemberMemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByMemberMemberIdAndTypeAndCreatedAtBetween(
            Long memberId, String type, LocalDateTime from, LocalDateTime to);
}

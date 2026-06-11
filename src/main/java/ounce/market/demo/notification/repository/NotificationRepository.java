package ounce.market.demo.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
}

package ounce.market.demo.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.delivery.entity.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery,Long> {
}

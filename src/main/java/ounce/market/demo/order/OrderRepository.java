package ounce.market.demo.order;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order,Long> {
}

package ounce.market.demo.order.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.product.entity.Product;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

}

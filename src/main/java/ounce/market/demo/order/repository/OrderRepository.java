package ounce.market.demo.order.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import ounce.market.demo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "delivery"})
    List<Order> findAllByMemberMemberIdOrderByOrderIdDesc(Long memberId);
}

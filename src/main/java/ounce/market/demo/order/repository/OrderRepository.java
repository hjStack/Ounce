package ounce.market.demo.order.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import ounce.market.demo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.order.entity.OrderStatus;


import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "delivery"})
    List<Order> findAllByMemberMemberIdOrderByOrderIdDesc(Long memberId);

    //grade
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.member.memberId = :memberId " +
            "AND o.status = OrderStatus.PAYMENT_COMPLETED")
    // 결제 완료된 주문만 등급 산정에 넣어야 함
    long sumTotalAmountByMemberId(@Param("memberId") Long memberId, OrderStatus status);
}

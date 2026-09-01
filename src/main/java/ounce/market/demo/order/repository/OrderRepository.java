
package ounce.market.demo.order.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.order.entity.OrderStatus;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "delivery"})
    List<Order> findAllByMemberMemberIdOrderByOrderIdDesc(Long memberId);

    /** 등급 산정용. 넘겨받은 상태의 주문 금액만 합산한다. */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.member.memberId = :memberId
              AND o.status = :status
            """)
    long sumTotalAmountByMemberId(@Param("memberId") Long memberId,
                                  @Param("status") OrderStatus status);
}
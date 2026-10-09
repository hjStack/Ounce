package ounce.market.demo.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.delivery.entity.Delivery;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @Query("SELECT d FROM Delivery d JOIN FETCH d.order o " +
            "WHERE o.member.memberId = :memberId ORDER BY d.deliveryId DESC")
    List<Delivery> findAllByOrderMemberMemberIdOrderByDeliveryIdDesc(@Param("memberId") Long memberId);

    @Query("SELECT d FROM Delivery d JOIN FETCH d.order o " +
            "WHERE d.deliveryId = :deliveryId AND o.member.memberId = :memberId")
    Optional<Delivery> findByDeliveryIdAndOrderMemberMemberId(@Param("deliveryId") Long deliveryId,
                                                              @Param("memberId") Long memberId);

    @Query("SELECT d FROM Delivery d JOIN FETCH d.order o " +
            "WHERE o.orderId = :orderId AND o.member.memberId = :memberId")
    Optional<Delivery> findByOrderOrderIdAndOrderMemberMemberId(@Param("orderId") Long orderId,
                                                                @Param("memberId") Long memberId);
}

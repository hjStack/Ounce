package ounce.market.demo.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.delivery.entity.Delivery;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findAllByOrderMemberMemberIdOrderByDeliveryIdDesc(Long memberId);

    Optional<Delivery> findByDeliveryIdAndOrderMemberMemberId(Long deliveryId, Long memberId);

    Optional<Delivery> findByOrderOrderIdAndOrderMemberMemberId(Long orderId, Long memberId);
}

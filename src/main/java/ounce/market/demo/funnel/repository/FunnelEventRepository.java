package ounce.market.demo.funnel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.funnel.entity.FunnelEvent;

public interface FunnelEventRepository extends JpaRepository<FunnelEvent, Long> {
}

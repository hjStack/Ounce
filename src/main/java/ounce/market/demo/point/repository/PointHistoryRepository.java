package ounce.market.demo.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.point.entity.PointHistory;


public interface PointHistoryRepository extends JpaRepository<PointHistory,Long> {
}

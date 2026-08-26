package ounce.market.demo.point.repository;

import ounce.market.demo.point.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findAllByMemberMemberIdOrderByPointHistoryIdDesc(Long memberId, Pageable pageable);
}

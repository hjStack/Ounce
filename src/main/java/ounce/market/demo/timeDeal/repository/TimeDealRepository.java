package ounce.market.demo.timeDeal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.timeDeal.entity.DealStatus;
import ounce.market.demo.timeDeal.entity.TimeDeal;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeDealRepository extends JpaRepository<TimeDeal, Long> {

    // 현재 시간에 유효하고, 상태가 READY나 IN_PROGRESS인 타임딜 상품만 가져오는 쿼리
    @Query("SELECT t FROM TimeDeal t JOIN FETCH t.product " +
            "WHERE t.status = :status " +
            "AND t.startTime <= :now AND t.endTime >= :now")
    List<TimeDeal> findActiveDealsWithProduct(@Param("status") DealStatus status, @Param("now") LocalDateTime now);

//    // 🧪 [임시 테스트용] 시간/상태 조건 없이 모든 딜을 product와 함께 조회
//    @Query("SELECT t FROM TimeDeal t JOIN FETCH t.product")
//    List<TimeDeal> findAllWithProductForTest();

    List<TimeDeal> findByStartTime(LocalDateTime startTime);

    boolean existsByStartTime(LocalDateTime startTime);
}
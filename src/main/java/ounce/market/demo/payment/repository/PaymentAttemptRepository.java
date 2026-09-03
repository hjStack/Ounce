package ounce.market.demo.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.payment.entity.PaymentAttempt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select a from PaymentAttempt a
            where a.cycle.cycleId = :cycleId
            order by a.attemptNo asc
            """)
    List<PaymentAttempt> findByCycle(@Param("cycleId") Long cycleId);

    /**
     * 대사 대상. 응답을 못 받았거나(UNKNOWN), 요청만 찍히고 결과가 안 들어온(PENDING) 시도.
     * 배치가 죽거나 PG가 타임아웃 나면 PENDING으로 영영 남기 때문에 둘 다 훑어야 한다.
     */
    @Query("""
            select a from PaymentAttempt a
            join fetch a.cycle c
            join fetch c.subscription
            where a.status in (
                    ounce.market.demo.payment.entity.PaymentStatus.UNKNOWN,
                    ounce.market.demo.payment.entity.PaymentStatus.PENDING)
              and a.requestedAt < :threshold
            order by a.requestedAt asc
            """)
    List<PaymentAttempt> findUnresolved(@Param("threshold") LocalDateTime threshold);
}
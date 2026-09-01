package ounce.market.demo.coupon.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.coupon.entity.CouponPolicy;

import java.util.Optional;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {

    Optional<CouponPolicy> findByCodeAndActiveTrue(String code);

    // 총 가격 보다 적은
    @Modifying(clearAutomatically = true)
    @Query("""
        update CouponPolicy p
           set p.issuedCount = p.issuedCount + 1
         where p.policyId = :policyId
           and (p.totalQuantity is null or p.issuedCount < p.totalQuantity)
        """)
    int increaseIssuedCount(@Param("policyId") Long policyId);
}
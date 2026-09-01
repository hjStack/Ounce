
package ounce.market.demo.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.coupon.entity.Coupon;
import ounce.market.demo.coupon.entity.CouponStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("select count(c) from Coupon c where c.member.memberId = :memberId and c.policy.policyId = :policyId")
    long countByMemberIdAndPolicyId(@Param("memberId") Long memberId, @Param("policyId") Long policyId);

    @Query("""
        select c from Coupon c
         where c.member.memberId = :memberId
         order by c.status asc, c.expiresAt asc
        """)
    List<Coupon> findAllByMemberId(@Param("memberId") Long memberId);

    @Query("""
        select c from Coupon c
         where c.member.memberId = :memberId
           and c.status = :status
        """)
    List<Coupon> findByMemberIdAndStatus(@Param("memberId") Long memberId,
                                         @Param("status") CouponStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Coupon c
           set c.status = ounce.market.demo.coupon.entity.CouponStatus.EXPIRED
         where c.status = ounce.market.demo.coupon.entity.CouponStatus.AVAILABLE
           and c.expiresAt is not null
           and c.expiresAt <= :now
        """)
    int expireAllBefore(@Param("now") LocalDateTime now);

    Optional<Coupon> findByCouponIdAndMemberMemberId(Long couponId, Long memberId);
}
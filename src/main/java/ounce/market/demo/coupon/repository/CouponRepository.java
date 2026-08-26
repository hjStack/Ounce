package ounce.market.demo.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.coupon.entity.Coupon;
import ounce.market.demo.coupon.entity.CouponStatus;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByMemberMemberIdOrderByCouponIdDesc(Long memberId);

    List<Coupon> findAllByMemberMemberIdAndStatusOrderByCouponIdDesc(Long memberId, CouponStatus status);

    Optional<Coupon> findByCouponIdAndMemberMemberId(Long couponId, Long memberId);
}

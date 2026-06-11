package ounce.market.demo.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.coupon.entity.Coupon;

public interface CouponRepository extends JpaRepository<Coupon,Long> {
}

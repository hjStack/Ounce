package ounce.market.demo.coupon.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.coupon.dto.request.CouponIssueRequest;
import ounce.market.demo.coupon.dto.response.CouponResponse;
import ounce.market.demo.coupon.dto.response.CouponValidationResponse;
import ounce.market.demo.coupon.entity.Coupon;
import ounce.market.demo.coupon.entity.CouponStatus;
import ounce.market.demo.coupon.entity.CouponUnavailableReason;
import ounce.market.demo.coupon.error.CouponErrorCode;
import ounce.market.demo.coupon.error.CouponException;
import ounce.market.demo.coupon.repository.CouponRepository;
import ounce.market.demo.delivery.entity.ShippingPolicy;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final ShippingPolicy shippingPolicy;

    public List<CouponResponse> getMyCoupons(Long memberId) {
        return couponRepository.findAllByMemberId(memberId).stream()
                .map(CouponResponse::from)
                .toList();
    }

    /** 사용 가능한 쿠폰을 할인액 큰 순으로 정렬해서 반환 */
    public List<CouponValidationResponse> getAvailableCoupons(Long memberId, int productAmount) {
        LocalDateTime now = LocalDateTime.now();
        int shippingFee = shippingPolicy.calculateShippingFee(productAmount, now);

        return couponRepository.findByMemberIdAndStatus(memberId, CouponStatus.AVAILABLE).stream()
                .map(coupon -> toValidationResponse(coupon, productAmount, shippingFee, now))
                .filter(CouponValidationResponse::available)
                .sorted(Comparator.comparingInt(
                        (CouponValidationResponse r) ->
                                r.productDiscountAmount() + r.shippingDiscountAmount()).reversed())
                .toList();
    }

    public CouponValidationResponse validateCoupon(Long memberId, Long couponId, int productAmount) {
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));

        // 남의 쿠폰이면 존재 자체를 숨긴다 (403 대신 404)
        if (!coupon.isOwnedBy(memberId)) {
            throw new CouponException(CouponErrorCode.COUPON_NOT_FOUND);
        }

        int shippingFee = shippingPolicy.calculateShippingFee(productAmount, now);
        return toValidationResponse(coupon, productAmount, shippingFee, now);
    }

    private CouponValidationResponse toValidationResponse(
            Coupon coupon, int productAmount, int shippingFee, LocalDateTime now) {

        CouponUnavailableReason reason = coupon.validateFor(productAmount, now);
        int productDiscount = coupon.calculateProductDiscount(productAmount, now);
        int shippingDiscount = coupon.calculateShippingDiscount(productAmount, shippingFee, now);

        return new CouponValidationResponse(
                coupon.getCouponId(),
                coupon.getName(),
                coupon.isAvailableFor(productAmount,now),
                coupon.getDiscountAmount(),
                coupon.getFinalAmount(),
                coupon.getShippingAmount());
    }
}
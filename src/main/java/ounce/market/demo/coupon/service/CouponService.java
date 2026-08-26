package ounce.market.demo.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.coupon.dto.request.CouponIssueRequest;
import ounce.market.demo.coupon.dto.response.CouponResponse;
import ounce.market.demo.coupon.dto.response.CouponValidationResponse;
import ounce.market.demo.coupon.entity.Coupon;
import ounce.market.demo.coupon.entity.CouponStatus;
import ounce.market.demo.coupon.entity.DiscountType;
import ounce.market.demo.coupon.repository.CouponRepository;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> getMyCoupons(String email) {
        Member member = getMemberByEmail(email);
        return couponRepository.findAllByMemberMemberIdOrderByCouponIdDesc(member.getMemberId())
                .stream()
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CouponValidationResponse> getAvailableCoupons(String email, int orderAmount) {
        Member member = getMemberByEmail(email);
        return couponRepository
                .findAllByMemberMemberIdAndStatusOrderByCouponIdDesc(member.getMemberId(), CouponStatus.AVAILABLE)
                .stream()
                .filter(coupon -> coupon.isAvailableFor(orderAmount))
                .map(coupon -> toValidationResponse(coupon, orderAmount))
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String email, Long couponId, int orderAmount) {
        Member member = getMemberByEmail(email);
        Coupon coupon = couponRepository.findByCouponIdAndMemberMemberId(couponId, member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        return toValidationResponse(coupon, orderAmount);
    }

    @Transactional
    public Long issueCoupon(CouponIssueRequest request) {
        validateCouponPolicy(request);

        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Coupon coupon = Coupon.builder()
                .member(member)
                .name(request.name())
                .discountType(request.discountType())
                .discountAmount(request.discountAmount())
                .maxDiscountAmount(request.maxDiscountAmount())
                .minOrderAmount(request.minOrderAmount())
                .expiresAt(request.expiresAt())
                .build();

        return couponRepository.save(coupon).getCouponId();
    }

    @Transactional
    public CouponResponse expireCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        coupon.expire();
        return CouponResponse.from(coupon);
    }

    private CouponValidationResponse toValidationResponse(Coupon coupon, int orderAmount) {
        boolean available = coupon.isAvailableFor(orderAmount);
        int discountAmount = available ? coupon.calculateDiscountAmount(orderAmount) : 0;
        return new CouponValidationResponse(
                coupon.getCouponId(),
                available,
                discountAmount,
                Math.max(orderAmount - discountAmount, 0)
        );
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    private void validateCouponPolicy(CouponIssueRequest request) {
        if (request.discountType() == DiscountType.PERCENT && request.discountAmount() > 100) {
            throw new IllegalArgumentException("정률 쿠폰 할인율은 100 이하이어야 합니다.");
        }
    }
}

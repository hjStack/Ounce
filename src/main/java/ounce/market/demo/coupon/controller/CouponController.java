package ounce.market.demo.coupon.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.coupon.dto.request.CouponValidateRequest;
import ounce.market.demo.coupon.dto.response.CouponResponse;
import ounce.market.demo.coupon.dto.response.CouponValidationResponse;
import ounce.market.demo.coupon.service.CouponService;

import java.util.List;

@Tag(name = "09. 쿠폰", description = "내 쿠폰 조회 및 쿠폰 검증")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    // todo
    // 쿠폰 발급 파이프라인을 이벤트 기반으로 만들었고,
    // 카프카의 at-least-once를 DB 멱등키로 정확히 한 번처럼 만들었다

    private final CouponService couponService;

    @GetMapping("/me")
    public ResponseEntity<List<CouponResponse>> getMyCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(couponService.getMyCoupons(userDetails.member().getMemberId()));
    }

    @GetMapping("/me/available")
    public ResponseEntity<List<CouponValidationResponse>> getAvailableCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int productAmount) {
        return ResponseEntity.ok(
                couponService.getAvailableCoupons(userDetails.member().getMemberId(), productAmount));
    }

    @PostMapping("/{couponId}/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long couponId,
            @Valid @RequestBody CouponValidateRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(
                userDetails.member().getMemberId(), couponId, request.orderAmount()));
    }
}
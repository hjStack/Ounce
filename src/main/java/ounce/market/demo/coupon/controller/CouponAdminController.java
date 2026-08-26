package ounce.market.demo.coupon.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.coupon.dto.request.CouponIssueRequest;
import ounce.market.demo.coupon.dto.response.CouponResponse;
import ounce.market.demo.coupon.service.CouponService;

@Tag(name = "09-1. 쿠폰 관리자", description = "쿠폰 발급 및 만료 처리")
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public ResponseEntity<Long> issueCoupon(@Valid @RequestBody CouponIssueRequest request) {
        Long couponId = couponService.issueCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(couponId);
    }

    @PatchMapping("/{couponId}/expire")
    public ResponseEntity<CouponResponse> expireCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.expireCoupon(couponId));
    }
}

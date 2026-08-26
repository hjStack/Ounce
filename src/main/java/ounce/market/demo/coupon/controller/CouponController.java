package ounce.market.demo.coupon.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.coupon.dto.request.CouponValidateRequest;
import ounce.market.demo.coupon.dto.response.CouponResponse;
import ounce.market.demo.coupon.dto.response.CouponValidationResponse;
import ounce.market.demo.coupon.service.CouponService;

import java.util.List;

@Tag(name = "09-0. 쿠폰", description = "내 쿠폰 조회 및 쿠폰 검증")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/me")
    public ResponseEntity<List<CouponResponse>> getMyCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(couponService.getMyCoupons(userDetails.getUsername()));
    }

    @GetMapping("/me/available")
    public ResponseEntity<List<CouponValidationResponse>> getAvailableCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int orderAmount) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(couponService.getAvailableCoupons(userDetails.getUsername(), orderAmount));
    }

    @PostMapping("/{couponId}/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long couponId,
            @Valid @RequestBody CouponValidateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(couponService.validateCoupon(
                userDetails.getUsername(),
                couponId,
                request.orderAmount()
        ));
    }
}

package ounce.market.demo.review.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.review.dto.request.ReviewCreateRequest;
import ounce.market.demo.review.dto.response.ReviewResponse;
import ounce.market.demo.review.service.ReviewService;

import java.util.List;

@Tag(name = "08. 리뷰", description = "리뷰 작성/조회/수정/삭제")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<Long> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request
    ) throws java.io.IOException {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = userDetails.member().getEmail();
        Long reviewId = reviewService.createReview(email, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
    }

    @PostMapping(value = "/api/products/{productId}/reviews", consumes = "multipart/form-data")
    public ResponseEntity<Long> createPhotoReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @RequestPart("request") @Valid ReviewCreateRequest request,
            @RequestPart("image") MultipartFile image
    ) throws java.io.IOException {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long reviewId = reviewService.createReview(
                userDetails.member().getEmail(), productId, request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = userDetails.member().getEmail();
        reviewService.deleteReview(email, reviewId);
        return ResponseEntity.noContent().build();
    }
}

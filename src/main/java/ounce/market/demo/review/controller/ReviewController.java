package ounce.market.demo.review.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.review.dto.request.ReviewCreateRequest;
import ounce.market.demo.review.dto.response.ReviewResponse;
import ounce.market.demo.review.service.ReviewService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<Long> createReview(
            @RequestParam Long memberId,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        Long reviewId = reviewService.createReview(memberId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

//    @Operation(summary = "내 리뷰 목록 조회")
//    @GetMapping("/api/members/me/reviews")
//    public ResponseEntity<List<ReviewResponse>> getMyReviews(
//            @PathVariable Long memberId,
//    ) {
//        return ResponseEntity.ok(reviewService.getMyReviews(memberId));
//    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @RequestParam Long memberId,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(memberId, reviewId);
        return ResponseEntity.noContent().build();
    }
}

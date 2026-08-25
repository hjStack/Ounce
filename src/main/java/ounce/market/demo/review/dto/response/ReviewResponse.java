package ounce.market.demo.review.dto.response;
import ounce.market.demo.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long productId,
        String writerName,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getProduct().getProductId(),
                review.getMember().getName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
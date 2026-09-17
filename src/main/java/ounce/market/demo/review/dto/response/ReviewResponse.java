package ounce.market.demo.review.dto.response;
import ounce.market.demo.review.entity.Review;
import ounce.market.demo.common.service.ImageUrlResolver;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long productId,
        Long memberId,
        String writerName,
        int rating,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getProduct().getProductId(),
                review.getMember().getMemberId(),
                review.getMember().getName(),
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }

    public static ReviewResponse from(Review review, ImageUrlResolver imageUrlResolver) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getProduct().getProductId(),
                review.getMember().getMemberId(),
                review.getMember().getName(),
                review.getRating(),
                review.getContent(),
                imageUrlResolver.toUrl(review.getImageUrl()),
                review.getCreatedAt()
        );
    }
}

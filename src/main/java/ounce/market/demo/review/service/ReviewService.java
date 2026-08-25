package ounce.market.demo.review.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.review.entity.Review;
import ounce.market.demo.review.repository.ReviewRepository;
import ounce.market.demo.review.dto.request.ReviewCreateRequest;
import ounce.market.demo.review.dto.response.ReviewResponse;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long createReview(Long memberId, Long productId, ReviewCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (reviewRepository.existsByProduct_ProductIdAndMember_MemberId(productId, memberId)) {
            throw new IllegalStateException("이미 이 상품에 리뷰를 작성했습니다.");
        }

        Review review = Review.builder()
                .product(product)
                .member(member)
                .rating(request.rating())
                .content(request.content())
                .build();

        return reviewRepository.save(review).getReviewId();
    }

    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProduct_ProductId(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    public List<ReviewResponse> getMyReviews(Long memberId) {
        return reviewRepository.findByMember_MemberId(memberId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("리뷰를 찾을 수 없습니다."));

        if (!review.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인이 작성한 리뷰만 접근할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }
}
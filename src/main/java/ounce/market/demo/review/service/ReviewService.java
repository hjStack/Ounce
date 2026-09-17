package ounce.market.demo.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.review.dto.request.ReviewCreateRequest;
import ounce.market.demo.review.dto.response.ReviewResponse;
import ounce.market.demo.review.entity.Review;
import ounce.market.demo.review.repository.ReviewRepository;
import ounce.market.demo.common.service.ImageUrlResolver;
import ounce.market.demo.common.service.S3UploadService;
import ounce.market.demo.point.service.PointService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PointService pointService;
    private final S3UploadService s3UploadService;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public Long createReview(String email, Long productId, ReviewCreateRequest request)
            throws java.io.IOException {
        return createReview(email, productId, request, null);
    }

    @Transactional
    public Long createReview(String email, Long productId, ReviewCreateRequest request,
                             MultipartFile image) throws java.io.IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (reviewRepository.existsByProductProductIdAndMemberMemberId(
                productId, member.getMemberId())) {
            throw new IllegalArgumentException("상품당 리뷰는 한 번만 작성할 수 있습니다.");
        }

        String imageUrl = null;
        boolean photoReview = image != null && !image.isEmpty();
        if (photoReview) imageUrl = s3UploadService.uploadImage(image);

        Review review = Review.builder()
                .product(product)
                .member(member)
                .rating(request.getRating())
                .content(request.getContent())
                .imageUrl(imageUrl)
                .build();

        Long reviewId = reviewRepository.save(review).getReviewId();
        pointService.grantReviewReward(member, photoReview ? 500 : 300,
                photoReview ? "사진 리뷰 작성 적립" : "리뷰 작성 적립");
        return reviewId;
    }

    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdWithMember(productId).stream()
                .map(review -> ReviewResponse.from(review, imageUrlResolver))
                .toList();
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("리뷰를 찾을 수 없습니다."));

        if (!review.getMember().getMemberId().equals(member.getMemberId())) {
            throw new AccessDeniedException("본인이 작성한 리뷰만 접근할 수 있습니다.");
        }

        int reward = review.getImageUrl() == null || review.getImageUrl().isBlank() ? 300 : 500;
        pointService.revokeReviewReward(
                member,
                reward,
                review.getImageUrl() == null ? "리뷰 삭제로 적립금 회수" : "사진 리뷰 삭제로 적립금 회수");
        reviewRepository.delete(review);
    }
}

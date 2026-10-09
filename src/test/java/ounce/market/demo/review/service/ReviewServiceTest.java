package ounce.market.demo.review.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ounce.market.demo.common.service.ImageUrlResolver;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.point.repository.PointHistoryRepository;
import ounce.market.demo.point.service.PointService;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.review.entity.Review;
import ounce.market.demo.review.repository.ReviewRepository;
import ounce.market.demo.common.service.S3UploadService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PointService pointService;
    @Mock private S3UploadService s3UploadService;
    @Mock private ImageUrlResolver imageUrlResolver;
    @Mock private PointHistoryRepository pointHistoryRepository;
    @Mock private Review review;
    @Mock private Product product;
    @Mock private Member member;

    private ReviewService service() {
        return new ReviewService(reviewRepository, productRepository, memberRepository,
                pointService, s3UploadService, imageUrlResolver, pointHistoryRepository);
    }

    @Test
    void reviewsByProduct_usesProductAndMemberFetchJoinQuery() {
        given(reviewRepository.findByProductIdWithMember(1L)).willReturn(List.of(review));
        stubReview();
        given(imageUrlResolver.toUrl(null)).willReturn(null);

        assertEquals(1, service().getReviewsByProduct(1L).size());

        verify(reviewRepository).findByProductIdWithMember(1L);
    }

    @Test
    void allReviews_usesProductAndMemberFetchJoinQuery() {
        given(reviewRepository.findAllWithMemberAndProduct(Sort.by(Sort.Direction.DESC, "createdAt")))
                .willReturn(List.of(review));
        stubReview();

        assertEquals(1, service().getAllReviews().size());

        verify(reviewRepository).findAllWithMemberAndProduct(
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void stubReview() {
        given(review.getProduct()).willReturn(product);
        given(review.getMember()).willReturn(member);
        given(product.getProductId()).willReturn(1L);
        given(product.getName()).willReturn("상품");
        given(member.getMemberId()).willReturn(2L);
        given(member.getName()).willReturn("회원");
    }
}

package ounce.market.demo.review.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.review.entity.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByProduct_ProductIdAndMember_MemberId(Long productId, Long memberId);

    List<Review> findByProduct_ProductId(Long productId);
    List<Review> findByMember_MemberId(Long memberId);
}
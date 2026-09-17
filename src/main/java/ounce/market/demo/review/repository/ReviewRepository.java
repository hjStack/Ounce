package ounce.market.demo.review.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.review.entity.Review;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByProductProductIdAndMemberMemberId(Long productId, Long memberId);

    @Query("select r from Review r join fetch r.member where r.product.productId = :productId")
    List<Review> findByProductIdWithMember(@Param("productId") Long productId);

    @Query("select r from Review r join fetch r.member where r.member.memberId = :memberId")
    List<Review> findByMemberIdWithMember(@Param("memberId") Long memberId);
}

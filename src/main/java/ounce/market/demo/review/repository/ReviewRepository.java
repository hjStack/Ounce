package ounce.market.demo.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review,Long> {
}

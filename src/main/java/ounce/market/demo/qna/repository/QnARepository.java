package ounce.market.demo.qna.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.qna.entity.QnA;

public interface QnARepository extends JpaRepository<QnA,Long> {
}

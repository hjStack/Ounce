package ounce.market.demo.qna.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.qna.entity.QnA;
import ounce.market.demo.qna.entity.QnaStatus;

import java.util.Optional;

public interface QnaRepository extends JpaRepository<QnA, Long> {

    Page<QnA> findAllByMemberMemberIdOrderByQnaIdDesc(Long memberId, Pageable pageable);

    Optional<QnA> findByQnaIdAndMemberMemberId(Long qnaId, Long memberId);

    Page<QnA> findAllByOrderByQnaIdDesc(Pageable pageable);

    Page<QnA> findAllByStatusOrderByQnaIdDesc(QnaStatus status, Pageable pageable);
}

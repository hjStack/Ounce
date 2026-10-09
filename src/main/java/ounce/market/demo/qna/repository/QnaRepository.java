package ounce.market.demo.qna.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.qna.entity.QnA;
import ounce.market.demo.qna.entity.QnaStatus;

import java.util.Optional;

public interface QnaRepository extends JpaRepository<QnA, Long> {

    @Query(value = "SELECT q FROM QnA q JOIN FETCH q.member " +
            "WHERE q.member.memberId = :memberId ORDER BY q.qnaId DESC",
            countQuery = "SELECT COUNT(q) FROM QnA q WHERE q.member.memberId = :memberId")
    Page<QnA> findAllByMemberMemberIdOrderByQnaIdDesc(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT q FROM QnA q JOIN FETCH q.member " +
            "WHERE q.qnaId = :qnaId AND q.member.memberId = :memberId")
    Optional<QnA> findByQnaIdAndMemberMemberId(@Param("qnaId") Long qnaId,
                                               @Param("memberId") Long memberId);

    @Query(value = "SELECT q FROM QnA q JOIN FETCH q.member ORDER BY q.qnaId DESC",
            countQuery = "SELECT COUNT(q) FROM QnA q")
    Page<QnA> findAllByOrderByQnaIdDesc(Pageable pageable);

    @Query(value = "SELECT q FROM QnA q JOIN FETCH q.member " +
            "WHERE q.status = :status ORDER BY q.qnaId DESC",
            countQuery = "SELECT COUNT(q) FROM QnA q WHERE q.status = :status")
    Page<QnA> findAllByStatusOrderByQnaIdDesc(@Param("status") QnaStatus status, Pageable pageable);
}

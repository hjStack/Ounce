package ounce.market.demo.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ounce.market.demo.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Member> findByWithdrawnEmailAndDeletedAtAfter(
            String withdrawnEmail, LocalDateTime deletedAt);

    Page<Member> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Member> findAllByMidnightAlertEnabledTrue();

    @Query("select coalesce(sum(m.point), 0) from Member m")
    long sumAllPoints();

    List<Member> findMemberByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.memberId = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}

/*
todo 로그인시 멤버 쿼리가 3번 조회되는 n+1 문제 해결
 */

package ounce.market.demo.member.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ounce.market.demo.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    // @Query 테스트
//    @Query("select m.memberId from Member m where m.memberId = :memberId")
//    Optional<Member> findByMemberId(@Param("memberId") Long memberId);
}

/*
todo 로그인시 멤버 쿼리가 3번 조회되는 n+1 문제 해결하고 해결로 바꾸기 
 */
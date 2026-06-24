package ounce.market.demo.member.repository;

import org.springframework.stereotype.Repository;
import ounce.market.demo.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}

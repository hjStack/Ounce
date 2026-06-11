package ounce.market.demo.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.member.entity.Member;

public interface memberRepository extends JpaRepository<Member,Long> {
}

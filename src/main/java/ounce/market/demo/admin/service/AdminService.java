package ounce.market.demo.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.admin.dto.response.AdminMemberResponse;
import ounce.market.demo.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<AdminMemberResponse> getAdminMembers(Pageable pageable) {
        return memberRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminMemberResponse::from);
    }

    @Transactional(readOnly = true)
    public long getAdminTotalPoint() {
        return memberRepository.sumAllPoints();
    }
}

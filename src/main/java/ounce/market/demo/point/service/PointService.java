package ounce.market.demo.point.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.point.dto.request.PointChangeRequest;
import ounce.market.demo.point.dto.response.PointBalanceResponse;
import ounce.market.demo.point.dto.response.PointChangeResponse;
import ounce.market.demo.point.dto.response.PointHistoryResponse;
import ounce.market.demo.point.entity.PointHistory;
import ounce.market.demo.point.entity.PointType;
import ounce.market.demo.point.repository.PointHistoryRepository;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PointBalanceResponse getMyPoint(String email) {
        Member member = getMemberByEmail(email);
        return PointBalanceResponse.from(member);
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryResponse> getMyHistories(String email, Pageable pageable) {
        Member member = getMemberByEmail(email);
        return pointHistoryRepository
                .findAllByMemberMemberIdOrderByPointHistoryIdDesc(member.getMemberId(), pageable)
                .map(PointHistoryResponse::from);
    }

    @Transactional
    public PointChangeResponse grant(PointChangeRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        member.addPoint(request.amount());
        saveHistory(member, null, request.amount(), request.type(), request.description());
        return new PointChangeResponse(member.getMemberId(), request.amount(), member.getPoint());
    }

    @Transactional
    public PointChangeResponse deduct(PointChangeRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        member.deductPoint(request.amount());
        int signedAmount = -request.amount();
        saveHistory(member, null, signedAmount, request.type(), request.description());
        return new PointChangeResponse(member.getMemberId(), signedAmount, member.getPoint());
    }

    public void recordUse(Member member, Order order, int amount, String description) {
        saveHistory(member, order, -amount, PointType.USE, description);
    }

    private void saveHistory(Member member, Order order, int amount, PointType type, String description) {
        PointHistory pointHistory = PointHistory.builder()
                .member(member)
                .order(order)
                .amount(amount)
                .type(type)
                .description(description)
                .balanceAfter(member.getPoint())
                .build();

        pointHistoryRepository.save(pointHistory);
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}

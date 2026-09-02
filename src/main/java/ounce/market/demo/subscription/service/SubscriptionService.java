package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;

import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.dto.SubscriptionCreateRequest;
import ounce.market.demo.subscription.dto.SubscriptionResponse;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionErrorCode;
import ounce.market.demo.subscription.entity.SubscriptionStatus;
import ounce.market.demo.subscription.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    /** 해지되지 않은 상태들. 중복 구독 판정과 현재 구독 조회에 함께 쓴다. */
    private static final List<SubscriptionStatus> LIVE_STATUSES =
            List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAYMENT_FAILED);

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long subscribe(String email, SubscriptionCreateRequest request) {
        Member member = findMember(email);

        // 회원당 구독은 하나. 결제 실패로 멈춘 구독이 있어도 새로 만들지 않고 재개하게 한다.
        if (subscriptionRepository.existsByMemberMemberIdAndStatusIn(
                member.getMemberId(), LIVE_STATUSES)) {
            throw new SubscriptionException(SubscriptionErrorCode.ALREADY_SUBSCRIBED);
        }

        validateStartDate(request.startDate());

        Subscription subscription = Subscription.builder()
                .member(member)
                .mealsPerWeek(request.mealsPerWeek())
                .startDate(request.startDate())
                .build();

        return subscriptionRepository.save(subscription).getSubscriptionId();
    }

    public SubscriptionResponse getMySubscription(String email) {
        Member member = findMember(email);

        Subscription subscription = subscriptionRepository
                .findByMemberMemberIdAndStatusIn(member.getMemberId(), LIVE_STATUSES)
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        return SubscriptionResponse.from(subscription);
    }

    /** 해지 이력까지 포함한 전체 목록 */
    public List<SubscriptionResponse> getMySubscriptionHistory(String email) {
        Member member = findMember(email);

        return subscriptionRepository
                .findAllByMemberMemberIdOrderBySubscriptionIdDesc(member.getMemberId())
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @Transactional
    public void changeMealsPerWeek(String email, Long subscriptionId, int mealsPerWeek) {
        Subscription subscription = findOwnedSubscription(email, subscriptionId);
        subscription.changeMealsPerWeek(mealsPerWeek);
    }

    @Transactional
    public void cancel(String email, Long subscriptionId) {
        Subscription subscription = findOwnedSubscription(email, subscriptionId);
        subscription.cancel(LocalDate.now());
    }

    private Subscription findOwnedSubscription(String email, Long subscriptionId) {
        Member member = findMember(email);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 남의 구독이면 존재 자체를 숨긴다 (403 대신 404)
        if (!subscription.isOwnedBy(member.getMemberId())) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        return subscription;
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    private static void validateStartDate(LocalDate startDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new SubscriptionException(SubscriptionErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}
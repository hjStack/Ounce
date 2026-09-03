package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionCycle;
import ounce.market.demo.subscription.repository.SubscriptionCycleRepository;
import ounce.market.demo.subscription.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자가 화면에서 직접 일으키는 구독 변경.
 * 배치 경로(SubscriptionBillingService)와 같은 행을 건드리므로 낙관적 락 충돌이 날 수 있다.
 * 여기서는 예외를 그대로 올려 409로 내려보내고, 사용자가 다시 시도하게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionMenuService menuService;
    private final SubscriptionCycleRepository cycleRepository;
    private final Clock clock;

    /**
     * 구독 생성. 가입일이 곧 첫 결제일이라 시작일을 따로 받지 않는다.
     * 결제는 이 트랜잭션이 커밋된 뒤 SubscriptionCheckoutService가 이어서 한다.
     */
    @Transactional
    public Long start(Long memberId, int mealsPerWeek) {
        if (subscriptionRepository.existsActiveByMember(memberId)) {
            throw new SubscriptionException(SubscriptionErrorCode.ALREADY_SUBSCRIBED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.MEMBER_NOT_FOUND));

        Subscription subscription = subscriptionRepository.save(
                Subscription.start(member, mealsPerWeek, today()));

        // 첫 회차를 같은 트랜잭션에서 연다. 이게 있어야 곧바로 메뉴를 담고 결제할 수 있다.
        menuService.openCycleFor(subscription);

        return subscription.getSubscriptionId();
    }

    /** 이번 주만 쉬어가기. */
    @Transactional
    public void skipThisWeek(Long memberId, Long subscriptionId) {
        Subscription subscription = load(memberId, subscriptionId);
        subscription.skipThisCycle(now());
        syncOpenCycle(subscription);
    }

    /** 지정한 날짜까지 쉬어가기. resumeDate 당일에 결제가 재개된다. */
    @Transactional
    public void pauseUntil(Long memberId, Long subscriptionId, LocalDate resumeDate) {
        Subscription subscription = load(memberId, subscriptionId);
        subscription.pauseUntil(now(), resumeDate);
        syncOpenCycle(subscription);
    }

    /**
     * 구독의 다음 결제일이 밀렸으면 열려 있는 회차의 날짜도 같이 민다.
     * 안 맞춰두면 회차가 이미 지난 결제일을 들고 남아 화면과 배송 예정일이 어긋난다.
     * <p>
     * 해지는 이 경로를 타지 않는다. 결제일이 밀리는 게 아니라 없어지는 것이라
     * 날짜를 옮길 대상이 아니고, 회차 자체를 닫아야 한다.
     */
    private void syncOpenCycle(Subscription subscription) {
        cycleRepository.findDraft(subscription.getSubscriptionId())
                .ifPresent(cycle -> cycle.rescheduleTo(subscription.getNextBillingDate()));
    }

    @Transactional
    public void resume(Long memberId, Long subscriptionId) {
        Subscription subscription = load(memberId, subscriptionId);
        subscription.resume(today());
        syncOpenCycle(subscription);
    }

    @Transactional
    public void changeMealsPerWeek(Long memberId, Long subscriptionId, int mealsPerWeek) {
        load(memberId, subscriptionId).changeMealsPerWeek(now(), mealsPerWeek);
    }

    /** 결제 수단을 교체한 직후 호출. 다음 재시도 슬롯을 기다리지 않고 바로 시도한다. */
    @Transactional
    public void retryNow(Long memberId, Long subscriptionId) {
        load(memberId, subscriptionId).retryNow(now());
    }

    @Transactional
    public void cancel(Long memberId, Long subscriptionId) {
        load(memberId, subscriptionId).cancel(today());
        // 열려 있던 회차를 같이 닫는다. 안 닫으면 지난 결제일을 든 고아 회차가 남는다.
        cycleRepository.findDraft(subscriptionId).ifPresent(SubscriptionCycle::closeOnCancel);
    }

    private Subscription load(Long memberId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        if (!subscription.isOwnedBy(memberId)) {
            // 남의 구독을 해지시키는 걸 막는다. 404로 내려서 존재 여부도 노출하지 않는 편이 낫다.
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        return subscription;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
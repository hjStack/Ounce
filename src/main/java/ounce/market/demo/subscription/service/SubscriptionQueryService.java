package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.dto.response.SubscriptionResponse;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.repository.SubscriptionCycleRepository;
import ounce.market.demo.subscription.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 조회 전용. 커맨드 서비스와 분리한 이유는 트랜잭션 성격이 다르기 때문이다.
 * 여기는 전부 readOnly이고, 상태를 바꾸는 메서드가 섞이면 실수로 쓰기가 들어간다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCycleRepository cycleRepository;
    private final Clock clock;

    public List<SubscriptionResponse.Detail> findMine(Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return subscriptionRepository.findAllByMember(memberId).stream()
                .map(s -> SubscriptionResponse.Detail.from(s, now))
                .toList();
    }

    public SubscriptionResponse.Detail findOne(Long memberId, Long subscriptionId) {
        return SubscriptionResponse.Detail.from(load(memberId, subscriptionId), LocalDateTime.now(clock));
    }

    /** 회차 이력. 소유권을 먼저 확인하고 나서 조회한다. */
    public List<SubscriptionResponse.Cycle> findCycles(Long memberId, Long subscriptionId) {
        load(memberId, subscriptionId);
        return cycleRepository.findHistory(subscriptionId).stream()
                .map(SubscriptionResponse.Cycle::from)
                .toList();
    }

    private Subscription load(Long memberId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        if (!subscription.isOwnedBy(memberId)) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        return subscription;
    }
}
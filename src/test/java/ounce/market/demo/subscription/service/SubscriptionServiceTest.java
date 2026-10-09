package ounce.market.demo.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.repository.SubscriptionCycleRepository;
import ounce.market.demo.subscription.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SubscriptionMenuService menuService;
    @Mock private SubscriptionCycleRepository cycleRepository;
    @Mock private Member member;
    @Mock private Subscription subscription;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC);
        service = new SubscriptionService(subscriptionRepository, memberRepository,
                menuService, cycleRepository, clock);
    }

    @Test
    void start_rejectsAlreadySubscribedMember() {
        given(subscriptionRepository.existsActiveByMember(1L)).willReturn(true);

        SubscriptionException exception = assertThrows(SubscriptionException.class,
                () -> service.start(1L, 5));

        assertEquals(SubscriptionErrorCode.ALREADY_SUBSCRIBED, exception.getErrorCode());
        verify(memberRepository, never()).findById(1L);
        verify(subscriptionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void start_rejectsUnknownMember() {
        given(subscriptionRepository.existsActiveByMember(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        SubscriptionException exception = assertThrows(SubscriptionException.class,
                () -> service.start(1L, 5));

        assertEquals(SubscriptionErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        verify(subscriptionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void change_isRejectedForAnotherMembersSubscription() {
        given(subscriptionRepository.findById(10L)).willReturn(Optional.of(subscription));
        given(subscription.isOwnedBy(1L)).willReturn(false);

        SubscriptionException exception = assertThrows(SubscriptionException.class,
                () -> service.cancel(1L, 10L));

        assertEquals(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND, exception.getErrorCode());
        verify(subscription, never()).cancel(TODAY);
        verify(cycleRepository, never()).findDraft(10L);
    }

    @Test
    void cancel_ownedSubscription_closesDraftCycle() {
        given(subscriptionRepository.findById(10L)).willReturn(Optional.of(subscription));
        given(subscription.isOwnedBy(1L)).willReturn(true);
        given(cycleRepository.findDraft(10L)).willReturn(Optional.empty());

        service.cancel(1L, 10L);

        verify(subscription).cancel(TODAY);
        verify(cycleRepository).findDraft(10L);
    }
}

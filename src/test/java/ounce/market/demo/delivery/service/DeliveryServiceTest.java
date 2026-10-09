package ounce.market.demo.delivery.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.delivery.repository.DeliveryRepository;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.order.entity.Order;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private Member member;
    @Mock
    private Delivery delivery;
    @Mock
    private Order order;

    private DeliveryService service() {
        return new DeliveryService(deliveryRepository, memberRepository);
    }

    @Test
    void myDeliveries_usesOrderFetchJoinQuery() {
        given(memberRepository.findByEmail("user@ounce.com")).willReturn(Optional.of(member));
        given(member.getMemberId()).willReturn(1L);
        given(deliveryRepository.findAllByOrderMemberMemberIdOrderByDeliveryIdDesc(1L))
                .willReturn(List.of(delivery));
        given(delivery.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn(10L);

        assertEquals(1, service().getMyDeliveries("user@ounce.com").size());

        verify(deliveryRepository).findAllByOrderMemberMemberIdOrderByDeliveryIdDesc(1L);
    }

    @Test
    void myDelivery_usesOrderFetchJoinQuery() {
        given(memberRepository.findByEmail("user@ounce.com")).willReturn(Optional.of(member));
        given(member.getMemberId()).willReturn(1L);
        given(deliveryRepository.findByDeliveryIdAndOrderMemberMemberId(20L, 1L))
                .willReturn(Optional.of(delivery));
        given(delivery.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn(10L);

        assertEquals(10L, service().getMyDelivery("user@ounce.com", 20L).orderId());

        verify(deliveryRepository).findByDeliveryIdAndOrderMemberMemberId(20L, 1L);
    }
}

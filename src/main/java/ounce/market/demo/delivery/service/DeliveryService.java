package ounce.market.demo.delivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.delivery.dto.request.DeliveryStatusUpdateRequest;
import ounce.market.demo.delivery.dto.request.DeliveryTrackingUpdateRequest;
import ounce.market.demo.delivery.dto.response.DeliveryResponse;
import ounce.market.demo.delivery.dto.response.DeliveryStatusResponse;
import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.delivery.repository.DeliveryRepository;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getMyDeliveries(String email) {
        Member member = getMemberByEmail(email);
        return deliveryRepository.findAllByOrderMemberMemberIdOrderByDeliveryIdDesc(member.getMemberId())
                .stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getMyDelivery(String email, Long deliveryId) {
        Member member = getMemberByEmail(email);
        Delivery delivery = deliveryRepository
                .findByDeliveryIdAndOrderMemberMemberId(deliveryId, member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getMyOrderDelivery(String email, Long orderId) {
        Member member = getMemberByEmail(email);
        Delivery delivery = deliveryRepository
                .findByOrderOrderIdAndOrderMemberMemberId(orderId, member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryStatusResponse updateStatus(Long deliveryId, DeliveryStatusUpdateRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        delivery.changeStatus(request.status());
        return DeliveryStatusResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse updateTracking(Long deliveryId, DeliveryTrackingUpdateRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        delivery.registerTracking(request.carrier(), request.trackingNumber());
        return DeliveryResponse.from(delivery);
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}

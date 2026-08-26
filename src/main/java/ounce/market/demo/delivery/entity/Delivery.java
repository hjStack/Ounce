package ounce.market.demo.delivery.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.order.entity.Order;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 필수
public class Delivery extends BaseEntity {

    public static final String SENDER = "Ounce";   // ✅ 고정 발신자

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;


    // 받는 사람 이름
    private String receiverName;

    // 받는 사람 주소
    private String address;

    private String receiverPhone;

    private String zipCode;

    private String addressDetail;

    private String trackingNumber;

    private String carrier;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) // NOT NULL 보장!
    private Order order;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;

    @Builder
    public Delivery(String receiverName, String address, String receiverPhone, String zipCode,
                    String addressDetail, DeliveryType deliveryType) {
        this.receiverName = receiverName;
        this.address = address;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.addressDetail = addressDetail;
        this.deliveryType = deliveryType;
        this.status = DeliveryStatus.PREPARING;      // 초기 상태
    }

    public void assignOrder(Order order) {
        this.order = order;
    }

    public void registerTracking(String carrier, String trackingNumber) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }

    public void changeStatus(DeliveryStatus status) {
        this.status = status;
        if (status == DeliveryStatus.SHIPPED && this.shippedAt == null) {
            this.shippedAt = LocalDateTime.now();
        }
        if (status == DeliveryStatus.DELIVERED && this.deliveredAt == null) {
            this.deliveredAt = LocalDateTime.now();
        }
    }
}

package ounce.market.demo.delivery.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.order.entity.Order;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 필수
public class Delivery {

    public static final String SENDER = "Ounce";   // ✅ 고정 발신자

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;


    // 받는 사람 이름
    private String receiverName;

    // 받는 사람 주소
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) // NOT NULL 보장!
    private Order order;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;


    @Builder
    public Delivery(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
        this.status = DeliveryStatus.PREPARING;      // 초기 상태
    }

    public void assignOrder(Order order) {
        this.order = order;
    }


}

package ounce.market.demo.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.delivery.entity.Delivery;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private int totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // 🔥mappedBy를 통해 "내 진짜 주인은 Delivery 테이블의 order 필드야"라고 선언합니다.
    // 🔥 cascade를 걸어두면 주문 저장 시 배송도 자동으로 한 방에 세이브됩니다!
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Delivery delivery;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items=new ArrayList<>();
    // OrderItem이 연관관계의 주인임

    @Builder
    public Order(int totalAmount, Member member, OrderStatus status) {
        this.totalAmount = totalAmount;
        this.member = member;
        this.status = status;
    }

    public void complete() {
        this.status = OrderStatus.PAYMENT_COMPLETED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }


    public void addOrderItem(OrderItem orderItem) {
        this.items.add(orderItem);
        orderItem.assignOrder(this);   // ✅ 주인 쪽 FK 세팅
    }

    public void AssignDelivery(Delivery delivery) {
        this.delivery = delivery;       // ① cascade 저장을 위한 연결
        delivery.assignOrder(this);     // ② FK(order_id) 채우기
    }
}

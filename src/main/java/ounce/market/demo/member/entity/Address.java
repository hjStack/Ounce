package ounce.market.demo.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    private String roadAddress;
    private String detailAddress;
    private boolean isDefault;

    private String recipientName;
    private String deliveryType;
}

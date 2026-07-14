package ounce.market.demo.cart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @JoinColumn(name = "quantity", nullable = false)
    private int quantity;

    @OneToMany(mappedBy = "cart")
    private List<CartProduct> cartItems = new ArrayList<>();  // ← 이 필드가 있어야 getCartItems() 생성됨

    @Builder
    public Cart(Member member) {
        this.member = member;
    }

}
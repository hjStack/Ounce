package ounce.market.demo.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import ounce.market.demo.cart.entity.CartProduct;

@Getter
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long cartId; // 수량 변경, 삭제할 때 필요한 고유 ID
    private Long productId;
    private String name;
    private Long basePrice; // ERD에 명시된 Product의 basePrice
    private String imageUrl; // ERD에 명시된 Product의 ImageUrl
    private int quantity;

    public static CartItemDto from(CartProduct cartItem) {
        return CartItemDto.builder()
                .productId(cartItem.getProduct().getProductId())
                .name(cartItem.getProduct().getName())
                .quantity(cartItem.getQuantity())
                .basePrice(cartItem.getProduct().getBasePrice())
                .build();
    }
}

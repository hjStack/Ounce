package ounce.market.demo.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.product.entity.Product;

@Getter
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long cartId; // 수량 변경, 삭제할 때 필요한 고유 ID
    private Long productId;
    private String name;
    private Long basePrice; // ERD에 명시된 Product의 basePrice
    private long finalPrice;     // 실제 적용 단가 (타임딜이면 할인가)
    private boolean timeDeal;    // 타임딜 적용 여부 (프론트 뱃지용)
    private String imageUrl; // ERD에 명시된 Product의 ImageUrl
    private int quantity;

    public static CartItemDto from(CartProduct cartItem,Integer discountRate) {

        Product product = cartItem.getProduct();
        long base = product.getBasePrice();
        boolean isDeal = (discountRate != null);

        long finalPrice = isDeal
                ? Math.round(base * (100 - discountRate) / 100.0)
                : product.getSalePrice();

        return CartItemDto.builder()
                .cartId(cartItem.getCartProductId())
                .productId(cartItem.getProduct().getProductId())
                .name(cartItem.getProduct().getName())
                .quantity(cartItem.getQuantity())
                .basePrice(cartItem.getProduct().getBasePrice())
                .finalPrice(finalPrice)
                .imageUrl(cartItem.getProduct().getImageUrl())
                .timeDeal(isDeal)
                .build();
    }
}

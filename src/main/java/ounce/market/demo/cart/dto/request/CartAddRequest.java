package ounce.market.demo.cart.dto.request;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartAddRequest {
    private Long productId;
    private int quantity;
}
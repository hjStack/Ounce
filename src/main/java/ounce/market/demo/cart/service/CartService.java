package ounce.market.demo.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.cart.entity.Cart;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.cart.repository.CartRepository;
import ounce.market.demo.cart.dto.response.CartItemDto;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartProductRepository cartProductRepository;

    // 💡 1. 내 장바구니 조회
    public List<CartItemDto> getCartItems(Long memberId) {

        Cart cart = cartRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        List<CartProduct> cartProducts = cartProductRepository.findByCartCartId(cart.getCartId());
        return cartProducts.stream()
                .map(cp -> new CartItemDto(
                        cp.getCartProductId(),
                        cp.getProduct().getProductId(),
                        cp.getProduct().getName(),
                        cp.getProduct().getBasePrice(),
                        cp.getProduct().getImageUrl(),
                        cp.getQuantity()
                ))
                .collect(Collectors.toList());
    }

}
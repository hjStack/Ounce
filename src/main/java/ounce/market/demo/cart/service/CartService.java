package ounce.market.demo.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.cart.entity.Cart;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.cart.repository.CartRepository;
import ounce.market.demo.cart.dto.response.CartItemDto;

import java.nio.file.AccessDeniedException;
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
                .map(CartItemDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateQuantity(Long memberId, Long cartProductId, int quantity) throws AccessDeniedException {
        // 1. 장바구니 아이템 조회
        CartProduct cartProduct = cartProductRepository.findById(cartProductId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 상품을 찾을 수 없습니다."));

        if (!cartProduct.getCart().getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인의 장바구니만 수정할 수 있습니다.");
        }

        // 3. 수량 변경
        cartProduct.updateQuantity(quantity);
        // JPA 변경 감지(dirty checking)로 자동 저장됨 (@Transactional)
    }

}
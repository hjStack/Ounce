package ounce.market.demo.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.cart.entity.Cart;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.cart.repository.CartRepository;
import ounce.market.demo.cart.dto.response.CartItemDto;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.timeDeal.entity.DealStatus;
import ounce.market.demo.timeDeal.entity.TimeDeal;
import ounce.market.demo.timeDeal.repository.TimeDealRepository;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartProductRepository cartProductRepository;
    private final ProductRepository productRepository;
    private final TimeDealRepository timeDealRepository;   // 👈 주입 추가


    @Transactional
    // 💡 1. 내 장바구니 조회
    public List<CartItemDto> getCartItems(Long memberId) {
        Cart cart = cartRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        List<CartProduct> cartProducts = cartProductRepository.findByCartCartId(cart.getCartId());

        Map<Long, Integer> dealRateMap = timeDealRepository
                .findActiveDealsWithProduct(DealStatus.IN_PROGRESS, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(
                        deal -> deal.getProduct().getProductId(),
                        TimeDeal::getDiscountRate,
                        (a, b) -> a   // 혹시 같은 상품 중복 딜이면 첫 번째
                ));

        return cartProducts.stream()
                .map(cp -> CartItemDto.from(cp, dealRateMap.get(cp.getProduct().getProductId())))
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
    }

    @Transactional
    public void addCartItem(Long memberId, Long productId, int quantity) {

        Cart cart = cartRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        Optional<CartProduct> existing = cartProductRepository.findByCartCartIdAndProductProductId(cart.getCartId(), productId);

        if (existing.isPresent()){
            existing.get().addQuantity(quantity);
        }

        else {
            // 없으면 → 새로 만들어 저장
            CartProduct newItem = CartProduct.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartProductRepository.save(newItem);
        }
    }
}
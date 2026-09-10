package ounce.market.demo.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.cart.dto.response.CartItemDto;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.cart.service.CartService;
import ounce.market.demo.member.repository.MemberRepository;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import ounce.market.demo.member.entity.Member;

/*
todo 7/2 -> 회원가입시 장바구니 즉시 생성 로직 작성 -> 완료
todo 7/30 cart n+1 해결하기 -> 완료
 */

@Tag(name = "03. 장바구니", description = "장바구니 조회, 회원가입시 장바구니 생성, 장바구니 삭제")
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final MemberRepository memberRepository;
    private final CartProductRepository cartProductRepository;

    @PostMapping("/items")
    @Operation(summary = "장바구니 상품 추가")
    public ResponseEntity<?> addCartItem(
            Authentication authentication,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        cartService.addCartItem(member.getMemberId(), productId, quantity);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "장바구니 조회", description = "로그인한 회원의 장바구니에 담긴 상품 목록을 반환합니다.")
    @GetMapping("/items")
    public ResponseEntity<?> getMyCartItems(Authentication authentication) {

        List<CartItemDto> cartItems = cartService.getCartItems(authentication.getName());
        return ResponseEntity.ok(cartItems);
    }

//    // 💡 2. 장바구니 상품 수량 변경
    @PatchMapping("/{cartId}")
    @Operation(summary = "장바구니 상품 수량 변경")
    public ResponseEntity<?> updateQuantity(
            Authentication authentication,
            @PathVariable("cartId") Long cartProductId,
            @RequestParam int quantity) throws AccessDeniedException {

        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow();

        // 서비스단에서 회원 ID와 상품 검증을 함께 처리
        cartService.updateQuantity(member.getMemberId(), cartProductId, quantity);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{cartItemId}")
    @Operation(summary = "장바구니 상품 삭제")
    public ResponseEntity<Void> deleteCartItem(
            Authentication authentication,
            @PathVariable Long cartItemId)  {

        Member member = memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));


        cartService.deleteCartItem(member.getMemberId(), cartItemId);
        return ResponseEntity.noContent().build();
    }
}
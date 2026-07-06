package ounce.market.demo.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.cart.dto.response.CartItemDto;
import ounce.market.demo.cart.service.CartService;
import ounce.market.demo.member.repository.MemberRepository;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import ounce.market.demo.member.entity.Member;


/*
todo 7/2 -> 회원가입시 장바구니 즉시 생성 로직 작성
 */

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final MemberRepository memberRepository;
    
    @GetMapping("/me")
    public ResponseEntity<?> getMyCartItems(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // 인증 객체에 저장된 유저 이메일로 memberId 조회
        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        List<CartItemDto> cartItems = cartService.getCartItems(member.getMemberId());
        return ResponseEntity.ok(cartItems);
    }

//    // 💡 2. 장바구니 상품 수량 변경 (PATCH /api/carts/{cartId}?quantity=X)
//    @PatchMapping("/{cartId}")
//    public ResponseEntity<?> updateQuantity(
//            Authentication authentication,
//            @PathVariable("cartId") Long cartProductId,
//            @RequestParam int quantity) {
//
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return ResponseEntity.status(401).build();
//        }
//
//        String email = authentication.getName();
//        Member member = memberRepository.findByEmail(email).orElseThrow();
//
//        // 서비스단에서 회원 ID와 상품 검증을 함께 처리
//        cartService.updateQuantity(member.getMemberId(), cartProductId, quantity);
//        return ResponseEntity.ok().build();
//    }
//
//    // 💡 3. 장바구니 상품 삭제 (DELETE /api/carts/{cartId})
//    @DeleteMapping("/{cartId}")
//    public ResponseEntity<?> removeCartItem(
//            Authentication authentication,
//            @PathVariable("cartId") Long cartProductId) {
//
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return ResponseEntity.status(401).build();
//        }
//
//        String email = authentication.getName();
//        Member member = memberRepository.findByEmail(email).orElseThrow();
//
//        cartService.removeCartItem(member.getMemberId(), cartProductId);
//        return ResponseEntity.ok().build();
//    }
}
package ounce.market.demo.order.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.order.dto.request.OrderCreateRequest;
import ounce.market.demo.order.dto.response.OrderResponse;
import ounce.market.demo.order.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Long> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody OrderCreateRequest request
            // TODO: 로그인 회원 id를 여기서 받아야 함 (아래 설명)
    ) {
        Long memberId = userDetails.member().getMemberId();
        Long orderId = orderService.createOrderFromCart(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.member().getMemberId();
        return ResponseEntity.ok(orderService.getMyOrders(memberId));
    }
}
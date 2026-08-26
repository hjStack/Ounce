package ounce.market.demo.order.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.order.dto.request.OrderCreateRequest;
import ounce.market.demo.order.dto.response.OrderResponse;
import ounce.market.demo.order.service.OrderService;

import java.util.List;

@Tag(name = "02. 주문", description = "주문 생성 및 조회")
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Long> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody OrderCreateRequest request
            // TODO: 로그인 회원 id를 여기서 받아야 함 -> 완료
    ) {
        String email = userDetails.getUsername();
        Long orderId = orderService.createOrderFromCart(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(orderService.getMyOrders(email));
    }
}

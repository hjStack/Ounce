package ounce.market.demo.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.product.repository.StockRedisRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ⚠️ 부하테스트 전용 - 테스트 끝나면 이 파일 삭제
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class LoadTestController {

    private final StockRedisRepository stockRedisRepository;

    @PostMapping("/stock-decrease")
    public ResponseEntity<Long> testStockDecrease() {
        try {
            long remaining = stockRedisRepository.decrease(1L, 1); // productId=1, qty=1
            return ResponseEntity.ok(remaining);          // 200 + 남은 재고
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 = 품절 (정상)
        }
    }
}
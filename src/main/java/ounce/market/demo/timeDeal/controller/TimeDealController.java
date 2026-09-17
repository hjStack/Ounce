package ounce.market.demo.timeDeal.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.product.dto.response.ProductResponse;
import ounce.market.demo.timeDeal.service.TimeDealService;
import ounce.market.demo.timeDeal.dto.TimeDealCreateCommand;
import ounce.market.demo.timeDeal.dto.TimeDealAdminResponse;

import java.util.List;

@Tag(name = "06. 미드나이트", description = "미드나이트 조회 및 구매 API")
@Slf4j
@RestController
@RequestMapping("/api/timedeal")
@RequiredArgsConstructor
public class TimeDealController {

    private final TimeDealService timeDealService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getTimeDeals() {
        // 서비스에서 10시~11시 검증을 마치고 데이터를 줍니다.
        return ResponseEntity.ok(timeDealService.getTodayTimeDealProducts());
    }

    // 관리자의 미드나이트 상품 등록
    @PostMapping("/admin")
    public ResponseEntity<Long> createTimeDeal(@RequestBody TimeDealCreateCommand command) {

        log.info("request start={}, end={}",
                command.startTime(), command.endTime());
        return ResponseEntity.ok(timeDealService.createTimeDeal(command));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<TimeDealAdminResponse>> getAdminTimeDeals() {
        return ResponseEntity.ok(timeDealService.getAdminTimeDeals());
    }

    @DeleteMapping("/admin/{timeDealId}")
    public ResponseEntity<Void> deleteTimeDeal(@PathVariable Long timeDealId) {
        timeDealService.deleteTimeDeal(timeDealId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/purchase/{productId}")
    public ResponseEntity<String> purchase(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();

        // 1. 비즈니스 로직(재고 차감, 결제)을 여기서 기다리지 않습니다!
        // 2. Kafka 토픽에 메시지(유저ID, 상품ID)만 툭 던집니다. (Publish)
        log.info("유저 [{}] 님의 상품 [{}] 구매 요청을 큐에 안전하게 적재했습니다.", userId, productId);

        // 3. 서버가 할 일을 다 했으니 유저에게 빛의 속도로 200 OK 응답을 줍니다.
        return ResponseEntity.ok("주문이 대기열에 성공적으로 접수되었습니다. 최종 결과는 3분 내로 '주문 내역'에서 확인해 주세요!");
    }
}

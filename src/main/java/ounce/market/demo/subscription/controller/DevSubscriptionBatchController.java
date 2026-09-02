package ounce.market.demo.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.subscription.service.CycleGenerationService;

import java.time.LocalDate;

/**
 * 배치를 손으로 돌려보기 위한 개발용 API.
 * 매일 새벽 3시를 기다릴 수 없으니 로컬에서만 열어둔다.
 * @Profile("local")이 붙어 있어 운영 프로파일에서는 빈 자체가 생성되지 않는다.
 */

@Profile("local")
@Tag(name = "99. 개발용 배치 트리거")
@RestController
@RequestMapping("/dev/subscriptions")
@RequiredArgsConstructor

// todo 구독 배치 서비스
// 매일 새벽 3시에 구독 스케줄러가 돔
public class DevSubscriptionBatchController {

    private final CycleGenerationService cycleGenerationService;

    @Operation(summary = "회차 생성 배치 수동 실행",
            description = "today를 미래 날짜로 넣으면 그 시점에 배치가 돈 것처럼 동작한다.")
    @PostMapping("/generate-cycles")
    public ResponseEntity<CycleGenerationService.CycleGenerationResult> generateCycles(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {

        LocalDate baseDate = (today != null) ? today : LocalDate.now();
        return ResponseEntity.ok(cycleGenerationService.generateCycles(baseDate));
    }
}
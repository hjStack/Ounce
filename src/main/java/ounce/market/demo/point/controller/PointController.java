package ounce.market.demo.point.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.point.dto.response.PointBalanceResponse;
import ounce.market.demo.point.dto.response.PointHistoryResponse;
import ounce.market.demo.point.service.PointService;

@Tag(name = "11-0. 포인트", description = "내 포인트 잔액 및 이력 조회")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/me")
    public ResponseEntity<PointBalanceResponse> getMyPoint(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(pointService.getMyPoint(userDetails.getUsername()));
    }

    @GetMapping("/me/histories")
    public ResponseEntity<Page<PointHistoryResponse>> getMyHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(pointService.getMyHistories(userDetails.getUsername(), pageable));
    }
}

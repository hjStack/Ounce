package ounce.market.demo.point.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.point.dto.request.PointChangeRequest;
import ounce.market.demo.point.dto.response.PointChangeResponse;
import ounce.market.demo.point.service.PointService;

@Tag(name = "11-1. 포인트 관리자", description = "포인트 지급 및 차감")
@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class PointAdminController {

    private final PointService pointService;

    @PostMapping("/grant")
    public ResponseEntity<PointChangeResponse> grant(@Valid @RequestBody PointChangeRequest request) {
        return ResponseEntity.ok(pointService.grant(request));
    }

    @PostMapping("/deduct")
    public ResponseEntity<PointChangeResponse> deduct(@Valid @RequestBody PointChangeRequest request) {
        return ResponseEntity.ok(pointService.deduct(request));
    }
}

package ounce.market.demo.qna.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.qna.dto.request.QnaCreateRequest;
import ounce.market.demo.qna.dto.request.QnaUpdateRequest;
import ounce.market.demo.qna.dto.response.QnaCreateResponse;
import ounce.market.demo.qna.dto.response.QnaResponse;
import ounce.market.demo.qna.service.QnaService;

@Tag(name = "12-0. Q&A", description = "1:1 문의 등록 및 조회")
@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @PostMapping
    public ResponseEntity<QnaCreateResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QnaCreateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        QnaCreateResponse response = qnaService.create(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<QnaResponse>> getMyQnas(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(qnaService.getMyQnas(userDetails.getUsername(), pageable));
    }

    @GetMapping("/{qnaId}")
    public ResponseEntity<QnaResponse> getMyQna(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long qnaId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(qnaService.getMyQna(userDetails.getUsername(), qnaId));
    }

    @PatchMapping("/{qnaId}")
    public ResponseEntity<QnaResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long qnaId,
            @Valid @RequestBody QnaUpdateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(qnaService.update(userDetails.getUsername(), qnaId, request));
    }

    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long qnaId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        qnaService.delete(userDetails.getUsername(), qnaId);
        return ResponseEntity.noContent().build();
    }
}

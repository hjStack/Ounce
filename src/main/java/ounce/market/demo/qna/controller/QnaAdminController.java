package ounce.market.demo.qna.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.qna.dto.request.QnaAnswerRequest;
import ounce.market.demo.qna.dto.response.QnaAnswerResponse;
import ounce.market.demo.qna.dto.response.QnaResponse;
import ounce.market.demo.qna.entity.QnaStatus;
import ounce.market.demo.qna.service.QnaService;

@Tag(name = "12-1. Q&A 관리자", description = "문의 목록 조회 및 답변")
@RestController
@RequestMapping("/api/admin/qna")
@RequiredArgsConstructor
public class QnaAdminController {

    private final QnaService qnaService;

    @GetMapping
    public ResponseEntity<Page<QnaResponse>> getAll(
            @RequestParam(required = false) QnaStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(qnaService.getAll(status, pageable));
    }

    @PostMapping("/{qnaId}/answer")
    public ResponseEntity<QnaAnswerResponse> answer(
            @PathVariable Long qnaId,
            @Valid @RequestBody QnaAnswerRequest request) {
        return ResponseEntity.ok(qnaService.answer(qnaId, request));
    }
}

package ounce.market.demo.qna.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QnaAnswerRequest(
        @NotBlank(message = "답변 내용은 필수입니다.")
        String answer
) {
}

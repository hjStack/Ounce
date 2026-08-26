package ounce.market.demo.qna.dto.response;

import ounce.market.demo.qna.entity.QnaStatus;

public record QnaCreateResponse(
        Long qnaId,
        QnaStatus status
) {
}

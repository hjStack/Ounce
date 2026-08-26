package ounce.market.demo.qna.dto.response;

import ounce.market.demo.qna.entity.QnA;
import ounce.market.demo.qna.entity.QnaStatus;

import java.time.LocalDateTime;

public record QnaAnswerResponse(
        Long qnaId,
        QnaStatus status,
        LocalDateTime answeredAt
) {
    public static QnaAnswerResponse from(QnA qna) {
        return new QnaAnswerResponse(
                qna.getQnaId(),
                qna.getStatus(),
                qna.getAnsweredAt()
        );
    }
}

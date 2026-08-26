package ounce.market.demo.qna.dto.response;

import ounce.market.demo.qna.entity.QnA;
import ounce.market.demo.qna.entity.QnaStatus;

import java.time.LocalDateTime;

public record QnaResponse(
        Long qnaId,
        Long memberId,
        String memberName,
        String title,
        String content,
        String category,
        QnaStatus status,
        String answer,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
    public static QnaResponse from(QnA qna) {
        Long memberId = qna.getMember() == null ? null : qna.getMember().getMemberId();
        String memberName = qna.getMember() == null ? null : qna.getMember().getName();

        return new QnaResponse(
                qna.getQnaId(),
                memberId,
                memberName,
                qna.getTitle(),
                qna.getContent(),
                qna.getCategory(),
                qna.getStatus(),
                qna.getAnswer(),
                qna.getCreatedAt(),
                qna.getAnsweredAt()
        );
    }
}

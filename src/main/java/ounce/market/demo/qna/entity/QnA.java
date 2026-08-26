package ounce.market.demo.qna.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnA extends BaseEntity {

    @Id
    @GeneratedValue
    private Long qnaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String title;

    @Lob
    private String content;

    private String category;

    @Lob
    private String answer;

    private LocalDateTime answeredAt;

    @Enumerated(EnumType.STRING)
    private QnaStatus status;

    @Builder
    public QnA(Member member, String title, String content, String category) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = QnaStatus.WAITING;
    }

    public void update(String title, String content, String category) {
        if (this.status == QnaStatus.ANSWERED) {
            throw new IllegalArgumentException("답변 완료된 문의는 수정할 수 없습니다.");
        }
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void answer(String answer) {
        this.answer = answer;
        this.status = QnaStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }
}

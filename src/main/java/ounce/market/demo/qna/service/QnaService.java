package ounce.market.demo.qna.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.qna.dto.request.QnaAnswerRequest;
import ounce.market.demo.qna.dto.request.QnaCreateRequest;
import ounce.market.demo.qna.dto.request.QnaUpdateRequest;
import ounce.market.demo.qna.dto.response.QnaAnswerResponse;
import ounce.market.demo.qna.dto.response.QnaCreateResponse;
import ounce.market.demo.qna.dto.response.QnaResponse;
import ounce.market.demo.qna.entity.QnA;
import ounce.market.demo.qna.entity.QnaStatus;
import ounce.market.demo.qna.repository.QnaRepository;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public QnaCreateResponse create(String email, QnaCreateRequest request) {
        Member member = getMemberByEmail(email);

        QnA qna = QnA.builder()
                .member(member)
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .build();

        QnA savedQna = qnaRepository.save(qna);
        return new QnaCreateResponse(savedQna.getQnaId(), savedQna.getStatus());
    }

    @Transactional(readOnly = true)
    public Page<QnaResponse> getMyQnas(String email, Pageable pageable) {
        Member member = getMemberByEmail(email);
        return qnaRepository.findAllByMemberMemberIdOrderByQnaIdDesc(member.getMemberId(), pageable)
                .map(QnaResponse::from);
    }

    @Transactional(readOnly = true)
    public QnaResponse getMyQna(String email, Long qnaId) {
        Member member = getMemberByEmail(email);
        QnA qna = qnaRepository.findByQnaIdAndMemberMemberId(qnaId, member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        return QnaResponse.from(qna);
    }

    @Transactional
    public QnaResponse update(String email, Long qnaId, QnaUpdateRequest request) {
        Member member = getMemberByEmail(email);
        QnA qna = qnaRepository.findByQnaIdAndMemberMemberId(qnaId, member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        qna.update(request.title(), request.content(), request.category());
        return QnaResponse.from(qna);
    }

    @Transactional
    public void delete(String email, Long qnaId) {
        Member member = getMemberByEmail(email);
        QnA qna = qnaRepository.findByQnaIdAndMemberMemberId(qnaId, member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        if (qna.getStatus() == QnaStatus.ANSWERED) {
            throw new IllegalArgumentException("답변 완료된 문의는 삭제할 수 없습니다.");
        }

        qnaRepository.delete(qna);
    }

    @Transactional(readOnly = true)
    public Page<QnaResponse> getAll(QnaStatus status, Pageable pageable) {
        Page<QnA> qnas = status == null
                ? qnaRepository.findAllByOrderByQnaIdDesc(pageable)
                : qnaRepository.findAllByStatusOrderByQnaIdDesc(status, pageable);

        return qnas.map(QnaResponse::from);
    }

    @Transactional
    public QnaAnswerResponse answer(Long qnaId, QnaAnswerRequest request) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        qna.answer(request.answer());
        return QnaAnswerResponse.from(qna);
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}

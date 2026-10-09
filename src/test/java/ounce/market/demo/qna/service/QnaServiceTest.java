package ounce.market.demo.qna.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.qna.entity.QnA;
import ounce.market.demo.qna.entity.QnaStatus;
import ounce.market.demo.qna.repository.QnaRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QnaServiceTest {

    @Mock
    private QnaRepository qnaRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private Member member;
    @Mock
    private QnA qna;

    private QnaService service() {
        return new QnaService(qnaRepository, memberRepository);
    }

    @Test
    void myQnas_usesMemberFetchJoinQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(memberRepository.findByEmail("user@ounce.com")).willReturn(Optional.of(member));
        given(member.getMemberId()).willReturn(1L);
        given(qnaRepository.findAllByMemberMemberIdOrderByQnaIdDesc(1L, pageable))
                .willReturn(new PageImpl<>(List.of(qna), pageable, 1));
        given(qna.getMember()).willReturn(member);
        given(member.getName()).willReturn("사용자");

        assertEquals(1, service().getMyQnas("user@ounce.com", pageable).getTotalElements());

        verify(qnaRepository).findAllByMemberMemberIdOrderByQnaIdDesc(1L, pageable);
    }

    @Test
    void adminQnas_usesMemberFetchJoinQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(qnaRepository.findAllByStatusOrderByQnaIdDesc(QnaStatus.WAITING, pageable))
                .willReturn(new PageImpl<>(List.of(qna), pageable, 1));
        given(qna.getMember()).willReturn(member);
        given(member.getName()).willReturn("관리대상 회원");

        assertEquals(1, service().getAll(QnaStatus.WAITING, pageable).getTotalElements());

        verify(qnaRepository).findAllByStatusOrderByQnaIdDesc(QnaStatus.WAITING, pageable);
    }
}

package ounce.market.demo.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ounce.market.demo.member.entity.Member;

// 🎁 서버 -> 프론트 (응답)
@Getter
@AllArgsConstructor
public class MemberResponse {
    private Long memberId;
    private String email;
    private int point;
    private String name;
    private String grade;

    // Entity -> DTO 변환 편의 메서드
    public static MemberResponse from(Member member,String grade) {

        return new MemberResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getPoint(),
                member.getName(),
                grade
        );
    }
}
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
    private int point; // 💡 화면에 보여줘야 하니까 여기엔 꼭 포함!

    private String name;

    // Entity -> DTO 변환 편의 메서드
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getPoint(),
                member.getName()
        );
    }
}
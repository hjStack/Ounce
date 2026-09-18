package ounce.market.demo.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ounce.market.demo.member.entity.Member;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminMemberResponse {
    private Long memberId;
    private String email;
    private String name;
    private int point;
    private String grade;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getMemberId(), member.getEmail(), member.getName(), member.getPoint(),
                member.getGrade() == null ? "BASIC" : member.getGrade(),
                member.getRole() == null ? "USER" : member.getRole().name(),
                member.getStatus() == null ? "ACTIVE" : member.getStatus().name(),
                member.getCreatedAt(), member.getUpdatedAt(),member.getDeletedAt()
        );
    }
}

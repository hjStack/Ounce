package ounce.market.demo.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminMemberPageResponse {
    private List<AdminMemberResponse> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private long totalPoint;

    public static AdminMemberPageResponse from(Page<AdminMemberResponse> page, long totalPoint) {
        return new AdminMemberPageResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), totalPoint
        );
    }
}

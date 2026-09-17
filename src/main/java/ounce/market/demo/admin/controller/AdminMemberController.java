package ounce.market.demo.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.admin.service.AdminService;
import ounce.market.demo.admin.dto.response.AdminMemberResponse;
import ounce.market.demo.admin.dto.response.AdminMemberPageResponse;

@Tag(name = "15.관리자 회원", description = "관리자 회원 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final AdminService AdminService;

    @GetMapping
    @Operation(summary = "회원 목록 조회")
    public ResponseEntity<AdminMemberPageResponse> getMembers(@PageableDefault(size = 20) Pageable pageable) {
        Page<AdminMemberResponse> members = AdminService.getAdminMembers(pageable);
        return ResponseEntity.ok(
                AdminMemberPageResponse.from(members, AdminService.getAdminTotalPoint())
        );
    }
}

package ounce.market.demo.member.controller;

import org.springframework.web.bind.annotation.RestController;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.ApiResponse;
import ounce.market.demo.notification.service.NotificationService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final NotificationService notificationService;

    @PostMapping("/api/v1/members/signup")
    public ApiResponse<String> signup() {
        log.info("1. 회원가입 핵심 로직 처리 중... (Thread: {})", Thread.currentThread().getName());

        // 2. 비동기 알림 발송 (여기서 멈추지 않고 바로 넘어갑니다!)
        notificationService.sendWelcomeEmail("new_user@ounce.com");

        log.info("3. 회원가입 완료. 클라이언트에게 응답 반환! (Thread: {})", Thread.currentThread().getName());

        return ApiResponse.created("회원가입이 완료되었습니다.", null);
    }
}
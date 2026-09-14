package ounce.market.demo.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public String healthCheck() {
        return "Ounce는 실행중입니다 ! "; // 화면 없이 텍스트(200 OK)만 반환
    }
}
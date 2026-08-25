package ounce.market.demo.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class TestController {

    @PostMapping("/api/test/delay")
    public String delayTest() throws InterruptedException {
        // 0.5초 동안 스레드를 꽉 잡고 놓아주지 않음 (DB I/O 시뮬레이션)
        Thread.sleep(500);
        return "ok";
    }
}

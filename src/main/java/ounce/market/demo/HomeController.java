package ounce.market.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.ApiResponse;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("Ounce 서버는 완벽하게 작동합니다");
    }
}
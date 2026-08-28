package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // 1. 관리자 대시보드 메인
    @GetMapping
    public String adminHome() {
        // templates/admin/index.html 파일을 보여줌
        return "admin/index";
    }

    // 2. 관리자 상품 목록
    @GetMapping("/products")
    public String adminProducts() {
        // templates/admin/products.html 파일을 보여줌
        return "admin/products";
    }
}

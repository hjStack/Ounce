package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // 1. 유저가 메뉴바에서 "로그인" 링크(/login)를 클릭하면 작동
    @GetMapping("/login")
    public String loginPage() {
        // templates 폴더 안의 login.html 껍데기를 브라우저로 렌더링!
        return "login";
    }

    // 2. 유저가 로그인 화면에서 "회원가입" 링크(/signup)를 클릭하면 작동
    @GetMapping("/signup")
    public String signupPage() {
        // templates 폴더 안의 signup.html 껍데기를 브라우저로 렌더링!
        return "signup";
    }
}
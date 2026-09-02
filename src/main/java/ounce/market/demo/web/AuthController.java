package ounce.market.demo.web;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    // 1. 유저가 메뉴바에서 "로그인" 링크(/login)를 클릭하면 작동
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String welcome, Model model) {
        if ("true".equals(welcome)) {
            model.addAttribute("message", "가입을 환영합니다! 축하 포인트 1,000P가 지급되었어요 🎁");
            model.addAttribute("type", "success");
        }
        return "login";  // 실제 로그인 뷰 이름에 맞게
    }

    // 2. 유저가 로그인 화면에서 "회원가입" 링크(/signup)를 클릭하면 작동
    @GetMapping("/signup")
    public String signupPage(@RequestParam(required = false) String welcome, Model model) {
        // templates 폴더 안의 signup.html 껍데기를 브라우저로 렌더링!
        if ("true".equals(welcome)) {
            model.addAttribute("message", "가입을 환영합니다! 무료 배송 쿠폰이 지급되었어요 🎁");
            model.addAttribute("type", "success");
        }
        return "signup";
    }
}
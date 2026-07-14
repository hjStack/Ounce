package ounce.market.demo.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MypageViewController {

    @GetMapping("/account") // 💡 내비바의 '/account' 링크와 주소를 일치시킵니다.
    public String mypageView() {
        return "mypage"; // ➡️ src/main/resources/templates/mypage.html 을 찾아가서 열어줍니다!
    }
}
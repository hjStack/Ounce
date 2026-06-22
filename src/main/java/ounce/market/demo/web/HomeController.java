package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("isLoggedIn", false);
        model.addAttribute("footerLinks", List.of(
                Map.of("title", "쇼핑", "links", List.of("전체 상품", "타임딜", "베스트", "신상품")),
                Map.of("title", "고객센터", "links", List.of("공지사항", "1:1 문의", "자주 묻는 질문")),
                Map.of("title", "회사소개", "links", List.of("브랜드 스토리", "채용", "제휴 문의")),
                Map.of("title", "정책", "links", List.of("이용약관", "개인정보처리방침", "교환/환불 정책"))
        ));
        return "home";
    }
}

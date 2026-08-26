package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminQnaViewController {

    /* 관리자 문의 관리 화면.
     *
     * 접근 제어는 SecurityConfig 의 /admin/** hasAuthority("ADMIN") 이 담당한다.
     * 화면 코드로 막지 않는 이유: 여기서 걸러도 데이터는 /api/admin/** 로 나가므로
     * 권한 판단이 두 군데로 갈리면 한쪽만 고쳐서 구멍이 난다. */
    @GetMapping("/admin/qna")
    public String adminQnaPage() {
        return "admin/qna";
    }
}

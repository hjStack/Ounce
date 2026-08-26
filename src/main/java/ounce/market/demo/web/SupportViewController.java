package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SupportViewController {

    /* 고객센터 — FAQ + 1:1 문의.
     *
     * 페이지 자체는 비로그인도 열려야 한다(SecurityConfig 에서 permitAll).
     * FAQ 로 답이 나오는 질문이 대부분인데 로그인 벽을 세우면 그 답을 못 보고
     * 이탈하거나 메일로 넘어간다. 반면 문의 목록·등록 API(/api/qna/**)는
     * 로그인이 필요하고, 화면은 401/403 을 받으면 로그인 안내로 바뛴다. */
    @GetMapping("/support")
    public String supportPage() {
        return "support";
    }
}

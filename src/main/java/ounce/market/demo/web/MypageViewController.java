package ounce.market.demo.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MypageViewController {

    @GetMapping("/account") // 💡 내비바의 '/account' 링크와 주소를 일치시킵니다.
    public String mypageView() {
        return "account"; // ➡️ src/main/resources/templates/mypage.html 을 찾아가서 열어줍니다!
    }

    @GetMapping("/orders")
    public String ordersView() {
        return "checkout";
    }

    @GetMapping("/products")
    public String productsView() {
        return "products";
    }

    @GetMapping("/timedeal")
    public String timeDealPage() {
        // src/main/resources/templates/timedeal.html 파일을 찾아서 렌더링하라는 뜻!
        return "timedeal";
    }
}
package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductViewController {

    // 브라우저에서 /products-detail/{id} 로 접속했을 때 이 메서드가 실행됨
    @GetMapping("/products-detail/{id}")
    public String getProductDetail(@PathVariable Long id, Model model) {

        // 여기에 상품 정보를 DB에서 조회하는 로직을 추가할 수 있습니다.
        model.addAttribute("productId", id);

        // templates/products-detail.html 파일을 화면에 보여줌 (확장자 생략)
        return "products-detail";
    }
}
package ounce.market.demo.web;

import lombok.RequiredArgsConstructor;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final int HOME_PRODUCT_LIMIT = 8;

    private final ProductRepository productRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<ounce.market.demo.product.entity.Product> products =
                productRepository.findByStatusIn(List.of(ProductStatus.ON_SALE, ProductStatus.TIME_DEAL));
        if (products.size() > HOME_PRODUCT_LIMIT) {
            products = products.subList(0, HOME_PRODUCT_LIMIT);
        }

        model.addAttribute("isLoggedIn", false);
        model.addAttribute("products", products);
        model.addAttribute("footerLinks", List.of(
                Map.of("title", "쇼핑", "links", List.of("전체 상품", "타임딜", "베스트", "신상품")),
                Map.of("title", "고객센터", "links", List.of("공지사항", "1:1 문의", "자주 묻는 질문")),
                Map.of("title", "회사소개", "links", List.of("브랜드 스토리", "채용", "제휴 문의")),
                Map.of("title", "정책", "links", List.of("이용약관", "개인정보처리방침", "교환/환불 정책"))
        ));
        return "home";
    }
}

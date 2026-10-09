package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.funnel.entity.FunnelEventType;
import ounce.market.demo.funnel.service.FunnelEventService;

import java.util.Map;

@Controller
public class ProductViewController {

    private final FunnelEventService eventService;

    public ProductViewController(FunnelEventService eventService) {
        this.eventService = eventService;
    }

    // 브라우저에서 /products-detail/{id} 로 접속했을 때 이 메서드가 실행됨
    @GetMapping("/products-detail/{id}")
    public String getProductDetail(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpServletRequest request, HttpServletResponse response) {

        // 여기에 상품 정보를 DB에서 조회하는 로직을 추가할 수 있습니다.
        model.addAttribute("productId", id);
        eventService.record(FunnelEventType.PRODUCT_VIEW, request, response,
                userDetails == null ? null : userDetails.member().getMemberId(),
                Map.of("product_id", id));

        // templates/products-detail.html 파일을 화면에 보여줌 (확장자 생략)
        return "products-detail";
    }
}

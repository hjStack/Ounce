package ounce.market.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class AccountController {

    @GetMapping("/account")
    public String account(Model model) {
        List<Map<String, Object>> recentOrders = List.of(
                Map.of(
                        "id", "ORD-20260618-01",
                        "statusLabel", "배송 완료",
                        "itemSummary", "콜드브루 원액 외 2건",
                        "orderedAt", "2026.06.18",
                        "finalPrice", 38500,
                        "firstItemName", "콜드브루 원액",
                        "image", "https://placehold.co/64x64"
                ),
                Map.of(
                        "id", "ORD-20260610-02",
                        "statusLabel", "배송중",
                        "itemSummary", "그래놀라 모닝박스",
                        "orderedAt", "2026.06.10",
                        "finalPrice", 24900,
                        "firstItemName", "그래놀라 모닝박스",
                        "image", "https://placehold.co/64x64"
                ),
                Map.of(
                        "id", "ORD-20260602-03",
                        "statusLabel", "배송 완료",
                        "itemSummary", "핸드드립 세트",
                        "orderedAt", "2026.06.02",
                        "finalPrice", 56000,
                        "firstItemName", "핸드드립 세트",
                        "image", "https://placehold.co/64x64"
                )
        );

        int totalSpent = recentOrders.stream()
                .mapToInt(o -> (Integer) o.get("finalPrice"))
                .sum();
        long deliveredCount = recentOrders.stream()
                .filter(o -> "배송 완료".equals(o.get("statusLabel")))
                .count();

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("currentUser", Map.of("name", "하예준", "email", "minnie908997@gmail.com"));
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("totalOrders", recentOrders.size());
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("couponCount", 3);

        return "account";
    }
}

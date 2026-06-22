package ounce.market.demo.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Member 인증이 아직 없어서 DB Cart 엔티티 대신 세션 기반 게스트 카트로 동작.
// 로그인 연동 후에는 세션 카트를 Cart/CartProduct로 옮기는 작업이 필요함.
@Controller
@RequiredArgsConstructor
public class CartController {

    private static final String SESSION_CART_KEY = "cart";
    private static final int FREE_SHIPPING_THRESHOLD = 30000;
    private static final int SHIPPING_FEE = 3000;

    private final ProductRepository productRepository;

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getSessionCart(HttpSession session) {
        Object cart = session.getAttribute(SESSION_CART_KEY);
        if (cart == null) {
            cart = new LinkedHashMap<Long, Integer>();
            session.setAttribute(SESSION_CART_KEY, cart);
        }
        return (Map<Long, Integer>) cart;
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId,
                       @RequestParam(required = false) String redirectTo,
                       HttpSession session) {
        Map<Long, Integer> cart = getSessionCart(session);
        cart.merge(productId, 1, (oldQty, one) -> Math.min(10, oldQty + 1));
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/products");
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long productId, @RequestParam int delta, HttpSession session) {
        Map<Long, Integer> cart = getSessionCart(session);
        cart.computeIfPresent(productId, (id, qty) -> Math.max(1, Math.min(10, qty + delta)));
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long productId, HttpSession session) {
        getSessionCart(session).remove(productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute(SESSION_CART_KEY);
        redirectAttributes.addFlashAttribute("toastMessage", "주문이 완료되었습니다! (주문 내역 연동은 준비 중입니다)");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:/";
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        Map<Long, Integer> sessionCart = getSessionCart(session);

        List<Product> cartProducts = productRepository.findAllById(sessionCart.keySet());
        List<Map<String, Object>> lines = cartProducts.stream()
                .map(product -> {
                    int quantity = sessionCart.get(product.getProductId());
                    Map<String, Object> line = new LinkedHashMap<>();
                    line.put("product", product);
                    line.put("quantity", quantity);
                    line.put("lineTotal", product.getBasePrice() * quantity);
                    return line;
                })
                .toList();

        int totalPrice = lines.stream().mapToInt(line -> (int) line.get("lineTotal")).sum();
        int shipping = lines.isEmpty() || totalPrice >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        int finalPrice = totalPrice + shipping;
        int totalItems = lines.stream().mapToInt(line -> (int) line.get("quantity")).sum();

        model.addAttribute("isLoggedIn", false);
        model.addAttribute("cartLines", lines);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shipping", shipping);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("amountToFreeShipping", Math.max(0, FREE_SHIPPING_THRESHOLD - totalPrice));
        return "cart";
    }
}

package ounce.market.demo.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartViewController {

    @GetMapping("/cart")
    public String cartPage() {
        return "cart"; // templates/cart.html 호출
    }
}
package ounce.market.demo.web;

import lombok.RequiredArgsConstructor;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("isLoggedIn", false);
        model.addAttribute("products",
                productRepository.findByStatusIn(List.of(ProductStatus.ON_SALE, ProductStatus.TIME_DEAL)));
        return "products";
    }
}

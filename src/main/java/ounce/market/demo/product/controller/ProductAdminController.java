package ounce.market.demo.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.product.service.ProductAdminService;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productAdminService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}

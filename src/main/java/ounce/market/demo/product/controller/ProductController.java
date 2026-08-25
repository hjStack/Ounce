package ounce.market.demo.product.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.product.dto.response.ProductResponse;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.service.ProductService;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Tag(name = "04. 상품", description = "상품 전체 조회 및 단건 조회")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    @GetMapping
    public Page<ProductResponse> getProducts(Pageable pageable) {
        // 고객 노출 상태만 (PREPARING, STOPPED 제외)
        List<ProductStatus> visible = List.of(
                ProductStatus.ON_SALE,
                ProductStatus.TIME_DEAL,
                ProductStatus.SOLD_OUT
        );

        return productRepository.findByStatusIn(visible, pageable)
                .map(ProductResponse::from);
    }

    // 상세 페이지
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
}
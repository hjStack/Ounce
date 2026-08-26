package ounce.market.demo.product.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.dto.response.ProductResponse;
import ounce.market.demo.product.dto.response.ProductSliceResponse;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.ProductRepositoryImpl;
import ounce.market.demo.product.service.ProductService;

import java.util.*;

@Tag(name = "04. 상품", description = "상품 조회")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    /** 고객 노출 상태 (PREPARING, STOPPED 제외) */
    private static final List<ProductStatus> VISIBLE_STATUSES = List.of(
            ProductStatus.VISIBLE,
            ProductStatus.TIME_DEAL,
            ProductStatus.SOLD_OUT
    );

    @GetMapping
    public ProductSliceResponse getProducts(ProductSearchCondition cond) {

        int size = cond.getSafeSize();
        List<Product> found = productRepository.search(cond, VISIBLE_STATUSES);

        boolean hasNext = found.size() > size;
        if (hasNext) {
            found = found.subList(0, size);
        }

        List<ProductResponse> products = found.stream()
                .map(ProductResponse::from)
                .toList();

        return new ProductSliceResponse(products, cond.getSafePage(), size, hasNext);
    }

    @GetMapping("/category-counts")
    public Map<String, Long> getCategoryCounts(@RequestParam(required = false) String keyword) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", productRepository .countAll(keyword, VISIBLE_STATUSES));
        counts.putAll(productRepository.countByCategory(keyword, VISIBLE_STATUSES));
        return counts;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
}
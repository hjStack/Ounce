package ounce.market.demo.product.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ounce.market.demo.product.dto.request.ProductCreateRequest;
import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.dto.response.ProductResponse;
import ounce.market.demo.product.dto.response.ProductSliceResponse;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.ProductRepositoryImpl;
import ounce.market.demo.product.service.ProductService;

import java.io.IOException;
import java.util.*;

@Tag(name = "04. 상품", description = "상품 조회")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
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

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE) // 💡 파일 업로드 명시!
    public ResponseEntity<Long> createProduct(@RequestPart("request") ProductCreateRequest request,
                                              @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        // 서비스 로직 실행 후, 방금 저장된 상품의 ID를 반환받음
        Long createdProductId = productService.createProduct(request,image);

        // 성공적으로 저장되었다면 HTTP 200 OK와 함께 생성된 ID를 프론트엔드에 응답
        return ResponseEntity.ok(createdProductId);
    }

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

    // 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId){

        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }


}
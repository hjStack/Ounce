package ounce.market.demo.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ounce.market.demo.product.dto.request.ProductCreateRequest;
import ounce.market.demo.product.service.ProductAdminService;
import ounce.market.demo.product.service.ProductService;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductAdminService productAdminService;
    private final ProductService productService;

    /**
     * 관리자 상품 등록.
     * 상품 정보와 이미지를 한 번에 받아 백엔드를 통해 이미지를 S3에 저장한다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> registerProduct(
            @RequestPart("request") ProductCreateRequest request,
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        return ResponseEntity.ok(productService.createProduct(request, image));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productAdminService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}

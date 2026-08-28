package ounce.market.demo.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.product.dto.request.ProductCreateRequest;
import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.dto.response.ProductSliceResponse;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductCategory;
import ounce.market.demo.product.entity.ProductStatus;
import ounce.market.demo.product.repository.CategoryRepository;
import ounce.market.demo.product.repository.ProductCategoryRepository;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.dto.response.ProductResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;


    // 상품 상세
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }


    @Transactional
    public Long createProduct(ProductCreateRequest request) {
        // 1. DTO를 Entity로 변환해서 Product 테이블에 저장 (이미지 URL도 함께 저장됨!)
        Product product = productRepository.save(request.toEntity());

        // 2. 전달받은 카테고리 ID들로 다대다(N:M) 연관관계 테이블에 매핑 저장
        categoryRepository.findAllById(request.getCategoryIds())
                .forEach(c -> productCategoryRepository.save(
                        // of() 대신 익숙한 builder()를 사용합니다!
                        ProductCategory.builder()
                                .product(product)
                                .category(c)
                                .build()
                ));

        return product.getProductId(); // 방금 생성된 상품의 번호 반환
    }
}
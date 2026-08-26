package ounce.market.demo.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.dto.response.ProductSliceResponse;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.CategoryRepository;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.dto.response.ProductResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;


    // 상품 상세
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }


//    public Long createProduct(productSearchCondition request) {
//        Product product = productRepository.save(request.toEntity());
//        categoryRepository.findAllById(request.getCategoryIds())
//                .forEach(c -> productCategoryRepository.save(ProductCategory.of(product, c)));
//        return product.getProductId();
//    }
}
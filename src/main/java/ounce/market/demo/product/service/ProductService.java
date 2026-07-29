package ounce.market.demo.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.dto.response.ProductResponse;

import java.awt.print.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 전체 상품 목록
    public List<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    // 상품 상세
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }
}
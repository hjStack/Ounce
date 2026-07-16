package ounce.market.demo.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.search.dto.*;
import ounce.market.demo.search.dto.response.ProductSearchResponse;
import ounce.market.demo.search.repository.ProductSearcher;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.engine", havingValue = "jpa", matchIfMissing = true)
public class JpaProductSearcher implements ProductSearcher {

    private final ProductRepository productRepository;

    @Override
    public Page<ProductSearchResponse> search(SearchCondition cond, Pageable pageable) {
        return productRepository
                .searchByKeyword(cond.keyword(), pageable)
                .map(ProductSearchResponse::from);
    }
}
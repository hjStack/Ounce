package ounce.market.demo.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ounce.market.demo.search.dto.SearchCondition;
import ounce.market.demo.search.dto.response.ProductSearchResponse;

public interface ProductSearcher {
    Page<ProductSearchResponse> search(SearchCondition cond, Pageable pageable);
}
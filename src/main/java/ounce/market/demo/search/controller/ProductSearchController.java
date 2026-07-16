package ounce.market.demo.search.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.search.dto.*;
import ounce.market.demo.search.dto.response.ProductSearchResponse;
import ounce.market.demo.search.repository.ProductSearcher;

@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearcher productSearcher;

    @GetMapping
    public Page<ProductSearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return productSearcher.search(
                new SearchCondition(keyword, minPrice, maxPrice, inStockOnly),
                PageRequest.of(page, size));
    }
}
package ounce.market.demo.search.dto;

public record SearchCondition(
        String keyword,
        Long minPrice,
        Long maxPrice,
        Boolean inStockOnly
) {}
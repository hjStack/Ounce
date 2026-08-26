package ounce.market.demo.product.repository;

import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;

import java.util.*;

public interface ProductRepositoryCustom {
    List<Product> search(ProductSearchCondition cond, List<ProductStatus> visible);
    Map<String, Long> countByCategory(String keyword, List<ProductStatus> visible);
    long countAll(String keyword, List<ProductStatus> visible);
}

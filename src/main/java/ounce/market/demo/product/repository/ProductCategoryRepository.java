package ounce.market.demo.product.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.product.entity.Category;
import ounce.market.demo.product.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    void deleteAllByProduct_ProductId(Long productId);

    // 단건 색인용
    @Query("select pc.category from ProductCategory pc where pc.product.productId = :productId")
    List<Category> findCategoriesByProductId(@Param("productId") Long productId);

    // 벌크 색인용 — N+1 방지의 핵심
    @Query("select pc from ProductCategory pc " +
            "join fetch pc.category " +
            "where pc.product.productId in :productIds")
    List<ProductCategory> findAllByProductIdIn(@Param("productIds") List<Long> productIds);
}

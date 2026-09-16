package ounce.market.demo.product.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom  {

    /**
     * 락 없이 재고를 원자적으로 차감한다.
     * 수정 행이 1이면 성공, 0이면 재고 부족 또는 이미 다른 요청이 차감한 경우다.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity " +
            "where p.productId = :productId and p.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("productId") Long productId,
                                 @Param("quantity") int quantity);

    List<Product> findByStatusIn(Collection<ProductStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // ✅ 이게 있어야 락임
    @Query("select p from Product p where p.productId in :id order by p.productId")
    List<Product> findByIdWithPessimisticLock(@Param("id") List<Long> ids);

    @Query("select p from Product p where p.productId > :lastId order by p.productId asc")
    List<Product> findForIndexing(@Param("lastId") Long lastId, Pageable pageable);

    // ES
    @Query("select p from Product p where p.name like %:keyword%")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

//    Page<Product> findByStatusIn(List<ProductStatus> status, Pageable pageable);

//    void deleteById(Long byId);

//    @Query("SELECT DISTINCT p FROM Product p " +
//            "JOIN ProductCategory pc ON pc.product = p " +
//            "JOIN pc.category c " +
//            "WHERE p.status IN :status AND c.key = :categoryKey")
//    Page<Product> findByStatusInAndCategoryKey(
//            @Param("status") List<ProductStatus> status,
//            @Param("categoryKey") String categoryKey,
//            Pageable pageable);
}

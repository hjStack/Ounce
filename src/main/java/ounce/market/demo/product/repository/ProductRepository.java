package ounce.market.demo.product.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.cart.entity.CartProduct;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatusIn(Collection<ProductStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // ✅ 이게 있어야 락임
    @Query("select p from Product p where p.productId in :id order by p.productId")
    List<Product> findByIdWithPessimisticLock(@Param("id") List<Long> ids);

    @Query("select p from Product p where p.productId > :lastId order by p.productId asc")
    List<Product> findForIndexing(@Param("lastId") Long lastId, Pageable pageable);

    // ES
    @Query("select p from Product p where p.name like %:keyword%")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

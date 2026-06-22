package ounce.market.demo.product.repository;

import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatusIn(Collection<ProductStatus> statuses);
}

package ounce.market.demo.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product,Long> {
}

package ounce.market.demo.product.repository;

import ounce.market.demo.product.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProduct_ProductId(Long productId);
}

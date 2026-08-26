package ounce.market.demo.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.product.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(String code);

    List<Category> findAllByOrderByIdAsc();
}
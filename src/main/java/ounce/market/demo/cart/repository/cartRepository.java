package ounce.market.demo.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.cart.entity.Cart;

public interface cartRepository extends JpaRepository<Cart,Long> {
}

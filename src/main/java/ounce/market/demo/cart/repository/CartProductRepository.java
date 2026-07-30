package ounce.market.demo.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ounce.market.demo.cart.entity.CartProduct;

import java.util.List;
import java.util.Optional;

// 2. CartProductRepository.java (아이템 찾기용)
public interface CartProductRepository extends JpaRepository<CartProduct, Long> {
    List<CartProduct> findByCartCartId(Long cartId);
//    List<CartProduct> findByCartProductId(Long cartProductId);
//
//    // 특정 장바구니(Cart)에 특정 상품(Product)이 이미 있는지 확인

    @Query("SELECT cp FROM CartProduct cp JOIN FETCH cp.product WHERE cp.cartProductId IN :ids")
    List<CartProduct> findByIdsWithProduct(@Param("ids") List<Long> ids);

    Optional<CartProduct> findByCartCartIdAndProductProductId(Long cartId, Long productId);
}
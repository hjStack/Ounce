package ounce.market.demo.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import ounce.market.demo.cart.entity.Cart;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCartId(Long cartId);

//    @Query("SELECT c FROM Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product " +
//            "WHERE c.member.email = :email")
//    Optional<Cart> findByMemberEmailWithItems(@Param("email") String email);

    Optional<Cart> findByMemberMemberId(Long memberMemberId);
}
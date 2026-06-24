//package ounce.market.demo.timeDeal.repository;
//
//import ounce.market.demo.timeDeal.entity.DealStatus;
//import ounce.market.demo.timeDeal.entity.TimeDeal;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface TimeDealRepository extends JpaRepository<TimeDeal, Long> {
//
//    Optional<TimeDeal> findByProduct_ProductIdAndStatus(Long productId, DealStatus status);
//}

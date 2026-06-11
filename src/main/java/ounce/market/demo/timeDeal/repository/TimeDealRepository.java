package ounce.market.demo.timeDeal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ounce.market.demo.timeDeal.entity.TimeDeal;

public interface TimeDealRepository extends JpaRepository<TimeDeal,Long> {
}

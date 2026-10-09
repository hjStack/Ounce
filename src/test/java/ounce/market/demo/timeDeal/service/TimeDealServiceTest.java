package ounce.market.demo.timeDeal.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.StockRedisRepository;
import ounce.market.demo.timeDeal.entity.TimeDeal;
import ounce.market.demo.timeDeal.repository.TimeDealRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeDealServiceTest {

    @Mock private TimeDealRepository timeDealRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockRedisRepository stockRedisRepository;
    @Mock private TimeDeal deal;

    private TimeDealService service() {
        return new TimeDealService(timeDealRepository, productRepository, stockRedisRepository);
    }

    @Test
    void openMidnightDeals_usesProductFetchJoinQuery() {
        LocalDateTime startTime = LocalDate.now(ZoneId.of("Asia/Seoul")).atTime(22, 0);
        given(timeDealRepository.findByStartTime(startTime)).willReturn(List.of(deal));

        service().openMidnightDeals();

        verify(timeDealRepository).findByStartTime(startTime);
        verify(deal).open();
    }

    @Test
    void closeMidnightDeals_usesProductFetchJoinQuery() {
        LocalDateTime startTime = LocalDate.now(ZoneId.of("Asia/Seoul")).atTime(22, 0);
        given(timeDealRepository.findByStartTime(startTime)).willReturn(List.of(deal));

        service().closeMidnightDeals();

        verify(timeDealRepository).findByStartTime(startTime);
        verify(deal).close();
    }
}

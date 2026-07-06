package ounce.market.demo.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.StockRedisRepository;

@Component
@RequiredArgsConstructor
public class StockInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final StockRedisRepository stockRedisRepository;

    @Override
    public void run(ApplicationArguments args) {
        productRepository.findAll().forEach(p ->
                stockRedisRepository.initStockIfAbsent(p.getProductId(), p.getStock()));
    }
}

/**

 5. 재고 초기화 (warm-up)
 Redis는 껐다 켜지거나 새로 배포되면 비어 있을 수 있으니,
 DB의 재고를 Redis로 밀어넣는 로직이 반드시 있어야 합니다.
 상품 등록·재입고 시점, 그리고 앱 기동 시 한 번.

 */
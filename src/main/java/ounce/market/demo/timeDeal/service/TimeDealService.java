//package ounce.market.demo.timeDeal.service;
//
//import lombok.RequiredArgsConstructor;
//import ounce.market.demo.product.entity.Product;
//import ounce.market.demo.product.entity.ProductStatus;
//import ounce.market.demo.product.entity.Stock;
//import ounce.market.demo.product.repository.ProductRepository;
//import ounce.market.demo.product.repository.StockRepository;
//import ounce.market.demo.timeDeal.dto.TimeDealCreateCommand;
//import ounce.market.demo.timeDeal.entity.TimeDeal;
//import ounce.market.demo.timeDeal.repository.TimeDealRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//public class TimeDealService {
//
//    private final TimeDealRepository timeDealRepository;
//    private final ProductRepository productRepository;
//    private final StockRepository stockRepository;
//
//    // 타임딜의 선착순 한도(maxPurchaseLimit)는 별도 카운터 없이, 등록 시점의 재고(Stock)로 통제한다는 전제.
//    // 즉 admin은 "이 타임딜에 100개만 풀겠다"는 의도를 반영해 Stock 수량을 100으로 맞춰서 등록해야 함.
//    @Transactional
//    public Long createTimeDeal(TimeDealCreateCommand command) {
//        Product product = productRepository.findById(command.productId())
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + command.productId()));
//
//        if (product.getStatus() != ProductStatus.ON_SALE) {
//            throw new IllegalStateException("정상 판매중인 상품만 타임딜로 등록할 수 있습니다. status=" + product.getStatus());
//        }
//
//        Stock stock = stockRepository.findByProduct_ProductId(product.getProductId())
//                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다. productId=" + product.getProductId()));
//
//        if (stock.getQuantity() < command.maxPurchaseLimit()) {
//            throw new IllegalArgumentException(
//                    "타임딜 한도가 남은 재고보다 많습니다. stock=" + stock.getQuantity() + ", limit=" + command.maxPurchaseLimit());
//        }
//
//        TimeDeal timeDeal = TimeDeal.builder()
//                .product(product)
//                .discountRate(command.discountRate())
//                .startTime(command.startTime())
//                .endTime(command.endTime())
//                .maxPurchaseLimit(command.maxPurchaseLimit())
//                .build();
//        timeDealRepository.save(timeDeal);
//
//        return timeDeal.getTimeDealId();
//    }
//
//    // READY -> IN_PROGRESS, 상품 상태도 TIME_DEAL로 전환 (밤 10시에 호출되는 부분)
//    @Transactional
//    public void open(Long timeDealId) {
//        TimeDeal timeDeal = timeDealRepository.findById(timeDealId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타임딜입니다. timeDealId=" + timeDealId));
//
//        if (LocalDateTime.now().isBefore(timeDeal.getStartTime())) {
//            throw new IllegalStateException("아직 타임딜 시작 시각이 아닙니다. startTime=" + timeDeal.getStartTime());
//        }
//
//        timeDeal.open();
//        timeDeal.getProduct().changeStatus(ProductStatus.TIME_DEAL);
//    }
//
//    // endTime이 지났는데 아직 재고가 남아 자연 종료되는 경우 (밤 12시에 호출되는 부분)
//    @Transactional
//    public void close(Long timeDealId) {
//        TimeDeal timeDeal = timeDealRepository.findById(timeDealId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타임딜입니다. timeDealId=" + timeDealId));
//        timeDeal.close();
//    }
//}

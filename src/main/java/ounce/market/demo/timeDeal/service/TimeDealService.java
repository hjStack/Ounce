package ounce.market.demo.timeDeal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.product.dto.response.ProductResponse;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.product.repository.StockRedisRepository;
import ounce.market.demo.timeDeal.entity.DealStatus;
import ounce.market.demo.timeDeal.entity.TimeDeal;
import ounce.market.demo.timeDeal.dto.TimeDealCreateCommand;
import ounce.market.demo.timeDeal.dto.TimeDealAdminResponse;
import ounce.market.demo.timeDeal.repository.TimeDealRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TimeDealRepository timeDealRepository;
    private final ProductRepository productRepository;
    private final StockRedisRepository stockRedisRepository;   // 👈 추가

    @Transactional
    public Long createTimeDeal(TimeDealCreateCommand command) {
        if (command.productId() == null) {
            throw new IllegalArgumentException("미드나이트 상품을 선택해주세요.");
        }
        if (command.discountRate() < 0 || command.discountRate() > 100) {
            throw new IllegalArgumentException("할인율은 0~100 사이여야 합니다.");
        }
        if (command.maxPurchaseLimit() <= 0) {
            throw new IllegalArgumentException("한정 수량은 1개 이상이어야 합니다.");
        }
        if (command.startTime() == null || command.endTime() == null
                || !command.startTime().isBefore(command.endTime())) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }

        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        TimeDeal deal = TimeDeal.builder()
                .product(product)
                .discountRate(command.discountRate())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .maxPurchaseLimit(command.maxPurchaseLimit())
                .build();
        TimeDeal saved = timeDealRepository.save(deal);
        stockRedisRepository.setStock(product.getProductId(), command.maxPurchaseLimit());
        return saved.getTimeDealId();
    }

    public List<TimeDealAdminResponse> getAdminTimeDeals() {
        return timeDealRepository.findAllWithProductOrderByStartTimeDesc()
                .stream()
                .map(TimeDealAdminResponse::from)
                .toList();
    }

    @Transactional
    public void deleteTimeDeal(Long timeDealId) {
        TimeDeal deal = timeDealRepository.findById(timeDealId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "미드나이트 상품을 찾을 수 없습니다."
                ));

        int deleted = timeDealRepository.deleteByIdIfExists(timeDealId);

        if (deleted == 1) {
            stockRedisRepository.deleteStock(deal.getProduct().getProductId());
        }
    }

    public List<ProductResponse> getTodayTimeDealProducts() {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalTime currentTime = now.toLocalTime();

        // 1. 시간 철통 방어: 22:00 ~ 23:00 사이가 아니면 빈 리스트 반환
        if (currentTime.isBefore(LocalTime.of(22, 0)) || currentTime.isAfter(LocalTime.of(23, 0))) {
            log.info("현재 시간 [{}] - 미드나이트 세일 시간이 아닙니다.", currentTime);
            return Collections.emptyList();
        }

        // 2. DB에서 현재 시간 기준 활성화된 타임딜 5개 가져오기
        List<TimeDeal> activeDeals = timeDealRepository.findActiveDealsWithProduct(DealStatus.IN_PROGRESS, now);
//        List<TimeDeal> activeDeals = timeDealRepository.findAllWithProductForTest();  // 🧪 임시

        // 3. TimeDeal 엔티티를 프론트엔드가 요구하는 ProductResponse DTO로 변환
        return activeDeals.stream()
                .map(deal -> {
                    Long productId = deal.getProduct().getProductId();
                    // Redis에서 현재 남은 재고 읽기 (없으면 한정수량으로 폴백)
                    int remaining = stockRedisRepository.getStock(productId)
                            .orElse(deal.getMaxPurchaseLimit());

                    return ProductResponse.builder()
                            .productId(productId)
                            .name(deal.getProduct().getName())
                            .basePrice(deal.getProduct().getBasePrice())
                            .salePrice(deal.getProduct().getSalePrice() * (100 - deal.getDiscountRate()) / 100)
                            .discountPercent(deal.getDiscountRate())
                            .imageUrl(deal.getProduct().getImageUrl())
                            .stock(remaining)                          // 👈 Redis 현재 재고
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional // DB에 Insert(저장)를 해야 하므로 트랜잭션 필수!
    @Scheduled(cron = "0 50 21 * * *", zone = "Asia/Seoul")
    public void generateMidnightDealsAutomatically() {
        log.info("🌙 [시스템] 미드나이트 세일 상품 자동 생성 스케줄러 기상!");

        LocalDate today = LocalDate.now(KST);
        LocalDateTime startTime = today.atTime(22, 0); // 오늘 밤 10시
        LocalDateTime endTime = today.atTime(23, 0);   // 오늘 밤 11시

        // 1. 혹시 이미 오늘 세팅된 타임딜이 있는지 갯수로 확인 (안전 방어막)
        // (실무에서는 중복 생성을 막기 위해 꼭 필요합니다)
        // timeDealRepository에 countByStartTime 쿼리를 만들어 써도 좋지만, 지금은 과감히 패스!
        if (timeDealRepository.existsByStartTime(startTime)) {
            log.warn("🚨 오늘 밤 10시 타임딜 상품이 이미 세팅되어 있습니다. 중복 생성을 방지합니다.");
            return;
        }

        // 2. 전체 상품 가져오기
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) {
            log.warn("🚨 등록된 상품이 없어 타임딜을 생성할 수 없습니다!");
            return;
        }

        // 3. 자바 메모리 단에서 안전하게 셔플 (랜덤 섞기)
        Collections.shuffle(allProducts);

        // 4. 앞에서부터 5개만 뽑아서 TimeDeal 엔티티로 조립
        List<TimeDeal> newDeals = allProducts.stream()
                .limit(5)
                .map(product -> TimeDeal.builder()
                        .product(product)
                        .discountRate(30)  //30퍼할인
                        .startTime(startTime)
                        .endTime(endTime)
                        .maxPurchaseLimit(50)  // 수량은 +50개

                        .build())
                .collect(Collectors.toList());

        // 5. DB에 한 번에 쾅! 저장 (Batch Insert)
        timeDealRepository.saveAll(newDeals);

        newDeals.forEach(deal ->
                stockRedisRepository.setStock(
                        deal.getProduct().getProductId(),
                        deal.getMaxPurchaseLimit()
                )
        );

        log.info("🌙 [시스템] 오늘 밤 10시를 위한 타임딜 상품 5개 세팅 완료! {}", newDeals.size());
    }

    @Transactional
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void openMidnightDeals() {
        LocalDateTime startTime = LocalDate.now(KST).atTime(22, 0);
        List<TimeDeal> deals = timeDealRepository.findByStartTime(startTime);
        deals.forEach(TimeDeal::open);
        log.info("🌙 [시스템] 미드나이트 세일 오픈! {}건 IN_PROGRESS 전환", deals.size());
    }

    // 🌙 매일 23:00 정각: IN_PROGRESS → CLOSED
    @Transactional
    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    public void closeMidnightDeals() {
        LocalDateTime startTime = LocalDate.now(KST).atTime(22, 0);
        List<TimeDeal> deals = timeDealRepository.findByStartTime(startTime);
        deals.forEach(TimeDeal::close);
        log.info("🌙 [시스템] 미드나이트 세일 종료! {}건 CLOSED 전환", deals.size());
    }
}

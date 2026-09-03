package ounce.market.demo.common.reader;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.subscription.entity.SubscriptionCycle.MenuLine;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.runner.ProductSnapshotReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 상품 정보를 회차 스냅샷으로 바꾼다.
 * <p>
 * getName()과 getPrice() 부분은 실제 Product 엔티티의 필드명에 맞춰야 한다.
 * 나머지 구조는 그대로 두면 된다.
 */
@Service
@RequiredArgsConstructor
public class JpaProductSnapshotReader implements ProductSnapshotReader {

    /** 메뉴를 안 고른 사용자에게 채워줄 기본 구성 후보. 실제로는 큐레이션 테이블에서 읽어야 한다. */
    private static final int DEFAULT_CANDIDATE_LIMIT = 10;

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuLine> readLines(Map<Long, Integer> selection) {
        List<Product> products = productRepository.findAllById(selection.keySet());

        // 요청한 상품 중 하나라도 없으면 전체를 거절한다.
        // 일부만 담아서 결제하면 사용자가 고른 것과 다른 박스가 나간다.
        if (products.size() != selection.size()) {
            throw new SubscriptionException(SubscriptionErrorCode.PRODUCT_NOT_AVAILABLE,
                    "requested=%d found=%d".formatted(selection.size(), products.size()));
        }

        List<MenuLine> lines = new ArrayList<>();
        for (Product product : products) {
            lines.add(toLine(product, selection.get(product.getProductId())));
        }
        return lines;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuLine> defaultLines(int mealsPerWeek) {
        List<Product> candidates = productRepository.findAll().stream()
                .limit(DEFAULT_CANDIDATE_LIMIT)
                .toList();

        if (candidates.isEmpty()) {
            throw new SubscriptionException(SubscriptionErrorCode.PRODUCT_NOT_AVAILABLE,
                    "기본 구성에 담을 상품이 없다");
        }

        // 후보를 돌아가며 1끼씩 배분한다. 5끼에 후보가 3개면 2/2/1이 된다.
        // 수량 합계가 mealsPerWeek과 정확히 맞아야 회차 검증을 통과한다.
        int[] quantities = new int[candidates.size()];
        for (int i = 0; i < mealsPerWeek; i++) {
            quantities[i % candidates.size()]++;
        }

        List<MenuLine> lines = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            if (quantities[i] > 0) {
                lines.add(toLine(candidates.get(i), quantities[i]));
            }
        }
        return lines;
    }

    /** 여기 두 줄만 실제 Product 엔티티에 맞춰 고치면 된다. */
    private MenuLine toLine(Product product, int quantity) {
        return new MenuLine(
                product.getProductId(),
                product.getName(),
                product.getBasePrice(),
                quantity);
    }
}
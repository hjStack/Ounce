package ounce.market.demo.subscription.runner;


import ounce.market.demo.subscription.entity.SubscriptionCycle;

import java.util.List;
import java.util.Map;

/**
 * 상품 정보를 읽어 회차에 담을 스냅샷 줄로 바꾸는 포트.
 * <p>
 * 구독 모듈이 상품 엔티티를 직접 알지 않도록 인터페이스로 끊는다.
 * 구현체는 상품 모듈에 두고, 판매 중지·품절 상품은 여기서 걸러야 한다.
 */
public interface ProductSnapshotReader {

    /**
     * 선택한 상품들의 현재 이름·가격을 읽어 온다.
     * 판매 중이 아닌 상품이 섞여 있으면 예외를 던져야 한다 —
     * 여기서 통과시키면 단종된 밀키트가 결제되고 배송 단계에서야 발견된다.
     *
     * @param selection 상품 ID -> 끼수
     */
    List<SubscriptionCycle.MenuLine> readLines(Map<Long, Integer> selection);

    /**
     * 큐레이션 기본 구성. 첫 회차이거나 이전 메뉴를 그대로 쓸 수 없을 때 채운다.
     * 수량 합계가 mealsPerWeek과 정확히 같아야 한다.
     */
    List<SubscriptionCycle.MenuLine> defaultLines(int mealsPerWeek);
}
package ounce.market.demo.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.subscription.entity.Subscription;
import ounce.market.demo.subscription.entity.SubscriptionCycle;
import ounce.market.demo.subscription.Exception.SubscriptionErrorCode;
import ounce.market.demo.subscription.Exception.SubscriptionException;
import ounce.market.demo.subscription.repository.SubscriptionCycleRepository;
import ounce.market.demo.subscription.repository.SubscriptionRepository;
import ounce.market.demo.subscription.runner.ProductSnapshotReader;

import java.util.List;
import java.util.Map;

/**
 * 회차 메뉴 관리.
 * <p>
 * 회차는 직전 결제가 끝나는 즉시 DRAFT로 열리고 다음 결제일 23시에 닫힌다.
 * 9/3에 결제했다면 그 자리에서 2회차가 열려 9/10 23시까지 메뉴를 바꿀 수 있다.
 * 창을 며칠 전에 여는 게 아니라 한 주 내내 열어두는 구조다.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionMenuService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCycleRepository cycleRepository;
    private final ProductSnapshotReader productReader;

    /**
     * 사용자가 다음 회차 메뉴를 고른다.
     * 수량 합계가 구독 끼수와 맞는지는 회차 엔티티가 판정한다.
     *
     * @param selection 상품 ID -> 끼수
     */
    @Transactional
    public void changeMenu(Long memberId, Long subscriptionId, Map<Long, Integer> selection,List<String> skippedDays) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        if (!subscription.isOwnedBy(memberId)) {
            // 남의 구독 메뉴를 바꾸는 걸 막는다. 존재 여부도 노출하지 않는다.
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        // 구독 주기 = 0주차
        SubscriptionCycle draft = cycleRepository.findDraft(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.MENU_NOT_EDITABLE,
                        "열린 회차 없음 subscriptionId=" + subscriptionId));

        draft.changeMenu(productReader.readLines(selection),skippedDays);

    }

    /**
     * 다음 회차를 연다. 결제 성공 직후와 안전망 배치가 호출한다.
     * 결제 트랜잭션과 분리해야 회차 개설 실패가 결제를 롤백시키지 않는다.
     */

    // 결제 성공후 1주차
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void openNextCycle(Long subscriptionId) {
        open(subscriptionRepository.findById(subscriptionId).orElseThrow());
    }

    /**
     * 구독 생성 트랜잭션 안에서 첫 회차를 여는 경로.
     * 새 트랜잭션으로 분리하면 아직 커밋되지 않은 구독이 안 보이므로 호출자의 트랜잭션에 참여한다.
     */
    @Transactional
    public void openCycleFor(Subscription subscription) {
        open(subscription);
    }

    /**
     * 실제 개설 로직.
     * <p>
     * 두 공개 메서드가 이 private 메서드를 부르는 형태인 게 중요하다.
     * openNextCycle이 openCycleFor를 직접 부르면 같은 빈 안의 self-invocation이라
     * 프록시를 안 거치고 @Transactional이 무시된다. 지금은 바깥에 트랜잭션이 있어
     * 우연히 돌아가지만, 우연에 기대는 코드는 남겨두지 않는다.
     */
    private void open(Subscription subscription) {
        if (!subscription.canOpenNextCycle()) {
            return;
        }
        // 이미 열려 있으면 건너뛴다. 안전망 배치가 매일 도는데 회차가 매일 열리면 안 된다.
        if (cycleRepository.findDraft(subscription.getSubscriptionId()).isPresent()) {
            return;
        }

        SubscriptionCycle cycle = SubscriptionCycle.openForMenu(
                subscription,
                subscription.nextCycleNumber(),
                subscription.getMealsPerWeek(),
                subscription.getNextBillingDate());

        applyDefaultMenu(subscription,cycle);
        cycleRepository.save(cycle);
    }

    /**
     * 기본 구성을 미리 채워둔다. 사용자가 아무것도 안 해도 배송은 나가야 한다.
     * 직전 회차와 같은 메뉴를 복사하고, 첫 회차이거나 끼수가 바뀌어 수량이 안 맞으면
     * 큐레이션 기본 구성으로 간다.
     */
    private void applyDefaultMenu(Subscription subscription, SubscriptionCycle cycle) {
        List<SubscriptionCycle.MenuLine> lines = cycleRepository
                .findLatestConfirmed(subscription.getSubscriptionId())
                .map(this::copyOf)
                .filter(previous -> totalQuantity(previous) == subscription.getMealsPerWeek())
                .orElseGet(() -> productReader.defaultLines(subscription.getMealsPerWeek()));

        cycle.changeMenu(lines, List.of());
    }

    private List<SubscriptionCycle.MenuLine> copyOf(SubscriptionCycle previous) {
        return previous.getItems().stream()
                .map(item -> new SubscriptionCycle.MenuLine(
                        item.getProductId(), item.getProductName(),
                        item.getUnitPrice(), item.getQuantity()))
                .toList();
    }

    private int totalQuantity(List<SubscriptionCycle.MenuLine> lines) {
        return lines.stream().mapToInt(SubscriptionCycle.MenuLine::quantity).sum();
    }
}
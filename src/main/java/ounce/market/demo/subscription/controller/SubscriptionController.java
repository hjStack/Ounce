package ounce.market.demo.subscription.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.subscription.dto.request.SubscriptionRequest;
import ounce.market.demo.subscription.dto.response.SubscriptionResponse;
import ounce.market.demo.subscription.runner.SubscriptionBillingRunner;
import ounce.market.demo.subscription.service.SubscriptionCheckoutService;
import ounce.market.demo.subscription.service.SubscriptionCheckoutService.CheckoutResult;
import ounce.market.demo.subscription.service.SubscriptionMenuService;
import ounce.market.demo.subscription.service.SubscriptionQueryService;
import ounce.market.demo.subscription.service.SubscriptionService;

import java.net.URI;
import java.util.List;

/**
 * 구독 API.
 * <p>
 * 회원 식별은 인증 주체에서만 꺼낸다. 요청 바디나 쿼리 파라미터로 memberId를 받는 순간
 * 남의 ID를 넣어 남의 구독을 해지시킬 수 있다.
 * <p>
 * memberId를 뽑는 방법은 {@link #memberId(CustomUserDetails)} 한 곳에만 있다.
 * 인증 방식을 바꾸더라도 고칠 곳이 그 메서드 하나여야 한다.
 */

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "구독",description="구독 등록 및 해지, 쉬어가기")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionQueryService queryService;
    private final SubscriptionMenuService menuService;
    private final SubscriptionCheckoutService checkoutService;

    /**
     * 구독 시작. 가입일이 곧 첫 결제일이라, 담은 메뉴로 그 자리에서 결제한다.
     * 23시 전이면 다음날 새벽, 넘기면 그 다음날 새벽에 배송된다.
     * <p>
     * 결제 실패도 201로 내려보낸다. 구독 자체는 만들어졌고,
     * 사용자는 결제 수단을 바꿔 재시도하면 된다.
     */
    @PostMapping
    public ResponseEntity<SubscriptionResponse.Checkout> subscribe(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SubscriptionRequest.Subscribe request) {

        Long memberId = memberId(user);

        log.info("memberId={}", memberId);
        CheckoutResult result = checkoutService.subscribeAndPay(
                memberId, request.mealsPerWeek(), request.selection());

        return ResponseEntity
                .created(URI.create("/api/subscriptions/" + result.subscriptionId()))
                .body(new SubscriptionResponse.Checkout(
                        result.outcome().name(),
                        checkoutService.messageOf(result.outcome()),
                        queryService.findOne(memberId, result.subscriptionId())));
    }

    @GetMapping("/me")
    public List<SubscriptionResponse.Detail> findMine(@AuthenticationPrincipal CustomUserDetails user) {
        return queryService.findMine(memberId(user));
    }

    @GetMapping("/{subscriptionId}")
    public SubscriptionResponse.Detail findOne(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        return queryService.findOne(memberId(user), subscriptionId);
    }

    /** 회차 이력. 결제·배송 내역 화면용. */
    @GetMapping("/{subscriptionId}/cycles")
    public List<SubscriptionResponse.Cycle> findCycles(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        return queryService.findCycles(memberId(user), subscriptionId);
    }

    /**
     * 이번 주만 쉬어가기. 결제일 23시 전까지만 가능하다.
     * 화면이 다음 결제일과 마감 시각을 다시 그려야 하므로 갱신된 구독을 돌려준다.
     */

    @PostMapping("/{subscriptionId}/skip")
    public SubscriptionResponse.Detail skip(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        Long memberId = memberId(user);
        subscriptionService.skipThisWeek(memberId, subscriptionId);
        return queryService.findOne(memberId, subscriptionId);
    }

    /** 배송 1회 건너뛰기 취소. 변경 마감 전까지만 원래 결제일로 복원한다. */
    @DeleteMapping("/{subscriptionId}/skip")
    public SubscriptionResponse.Detail cancelSkip(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        Long memberId = memberId(user);
        subscriptionService.cancelSkip(memberId, subscriptionId);
        return queryService.findOne(memberId, subscriptionId);
    }

    /** 지정한 날짜까지 쉬어가기. resumeDate 당일에 결제가 재개된다. */
    @PostMapping("/{subscriptionId}/pause")
    public SubscriptionResponse.Detail pause(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionRequest.Pause request) {

        Long memberId = memberId(user);
        subscriptionService.pauseUntil(memberId, subscriptionId, request.resumeDate());
        return queryService.findOne(memberId, subscriptionId);
    }

    /** 쉬어가기 즉시 해제. */
    @PostMapping("/{subscriptionId}/resume")
    public SubscriptionResponse.Detail resume(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        Long memberId = memberId(user);
        subscriptionService.resume(memberId, subscriptionId);
        return queryService.findOne(memberId, subscriptionId);
    }

    /** 끼수 변경. 마감 전이면 이번 회차부터, 지났으면 다음 회차부터 적용된다. */
    @PatchMapping("/{subscriptionId}/meals")
    public SubscriptionResponse.Detail changeMeals(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionRequest.ChangeMeals request) {

        Long memberId = memberId(user);
        subscriptionService.changeMealsPerWeek(memberId, subscriptionId, request.mealsPerWeek());
        return queryService.findOne(memberId, subscriptionId);
    }

    /**
     * 다음 회차 메뉴 선택. 직전 결제 직후 열려 다음 결제일 23시에 닫힌다.
     * 수량 합계가 구독 끼수와 같아야 한다.
     */
    @PutMapping("/{subscriptionId}/menu")
    public SubscriptionResponse.Detail changeMenu(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionRequest.ChangeMenu request) {

        Long memberId = memberId(user);
        menuService.changeMenu(memberId, subscriptionId, request.selection());
        return queryService.findOne(memberId, subscriptionId);
    }

    /**
     * 메뉴를 담고 그 자리에서 결제한다. 가입 시 결제가 실패했거나
     * 결제 실패로 쉬어간 주에 사용자가 직접 다시 결제할 때 쓴다.
     * <p>
     * 결제 실패도 200으로 내려보내고 결말을 본문에 담는다.
     * 카드 거절은 서버 오류가 아니라 화면이 안내해야 할 정상 결과다.
     */
    @PostMapping("/{subscriptionId}/checkout")
    public SubscriptionResponse.Checkout checkout(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionRequest.ChangeMenu request) {

        Long memberId = memberId(user);
        SubscriptionBillingRunner.ChargeOutcome outcome = checkoutService.checkoutNow(memberId, subscriptionId, request.selection());

        return new SubscriptionResponse.Checkout(
                outcome.name(),
                checkoutService.messageOf(outcome),
                queryService.findOne(memberId, subscriptionId));
    }

    /**
     * 결제 수단 교체 후 즉시 재시도.
     * 다음 재시도 슬롯(2시간 뒤)을 기다리지 않고 큐에 올린다.
     * 실제 결제는 스케줄러가 집어가므로 202를 돌려준다.
     */
    @PostMapping("/{subscriptionId}/payment-retry")
    public ResponseEntity<Void> retryPayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        subscriptionService.retryNow(memberId(user), subscriptionId);
        return ResponseEntity.accepted().build();
    }

    /** 해지. 멱등이므로 이미 해지된 구독에 다시 호출해도 204다. */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long subscriptionId) {

        subscriptionService.cancel(memberId(user), subscriptionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 인증 주체에서 회원 식별자를 꺼낸다.
     * <p>
     * CustomUserDetails가 들고 있는 Member는 UserDetailsService에서 조회된 뒤
     * 영속성 컨텍스트가 닫힌 detached 엔티티다. 여기서는 이미 로딩된 memberId만 꺼내 쓰고,
     * 이 Member 객체 자체를 서비스로 넘겨 연관관계에 붙이지 않는다.
     * 지연 로딩 필드를 건드리면 LazyInitializationException이 나고,
     * 로그인 시점 이후 바뀐 회원 정보를 덮어쓸 위험도 있다.
     * <p>
     * null 검사는 방어용이다. 실제 차단은 SecurityFilterChain에서 이 경로를
     * authenticated()로 막아야 한다 — 컨트롤러가 인증의 마지막 방어선이 되면 안 된다.
     */
    private Long memberId(CustomUserDetails user) {
        if (user == null || user.member() == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return user.member().getMemberId();
    }
}

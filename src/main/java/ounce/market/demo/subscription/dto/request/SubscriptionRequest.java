package ounce.market.demo.subscription.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ounce.market.demo.subscription.entity.Subscription;

import java.time.LocalDate;
import java.util.Map;

/**
 * 요청 DTO.
 * <p>
 * 형식 검증만 여기서 하고, 도메인 규칙은 Clock을 주입받는 엔티티가 최종 판정한다.
 * @Future는 서버 기본 시계로 판정하므로 정확한 경계를 맡길 수 없다.
 * 검증이 두 겹인 게 중복이 아니라, 빠른 거절과 불변식 보장이라는 다른 일을 한다.
 */
public final class SubscriptionRequest {

    private SubscriptionRequest() {
    }

    /**
     * 구독 신청. 시작일을 받지 않는다 — 가입일이 곧 첫 결제일이다.
     * 담은 메뉴까지 함께 받아 한 번에 결제한다.
     */
    public record Subscribe(
            @Min(value = Subscription.MIN_MEALS_PER_WEEK, message = "주당 끼니 수는 4끼 이상이어야 합니다.")
            @Max(value = Subscription.MAX_MEALS_PER_WEEK, message = "주당 끼니 수는 7끼 이하여야 합니다.")
            int mealsPerWeek,

            @NotEmpty(message = "밀키트를 한 개 이상 선택해야 합니다.")
            Map<Long, Integer> selection
    ) {
    }

    public record Pause(
            @NotNull(message = "쉬어가기 종료일은 필수입니다.")
            @Future(message = "쉬어가기 종료일은 오늘 이후여야 합니다.")
            LocalDate resumeDate
    ) {
    }

    /**
     * 메뉴 선택. 상품 ID -> 끼수.
     * 수량 합계 검증은 회차 엔티티가 한다 — 구독의 끼수를 알아야 판정할 수 있어서
     * DTO 단계에서는 불가능하다.
     */
    public record ChangeMenu(
            @NotEmpty(message = "밀키트를 한 개 이상 선택해야 합니다.")
            Map<Long, Integer> selection
    ) {
    }

    public record ChangeMeals(
            @Min(value = Subscription.MIN_MEALS_PER_WEEK, message = "주당 끼니 수는 4끼 이상이어야 합니다.")
            @Max(value = Subscription.MAX_MEALS_PER_WEEK, message = "주당 끼니 수는 7끼 이하여야 합니다.")
            int mealsPerWeek
    ) {
    }
}
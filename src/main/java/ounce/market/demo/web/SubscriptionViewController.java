package ounce.market.demo.web;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

@Controller
public class SubscriptionViewController {

    /* 주간 메뉴 변경 마감 시점.
     *
     * 아직 확정값이 아니다. docs/application-revised.md 는 "제조 일정이 허용하는 가장 늦은
     * 시점까지" 열어두겠다고만 하고, 정확한 마감은 위탁 제조사와 협의 후 확정이라고 적어두었다.
     * 그래서 요일·시각을 화면에 흘려놓지 않고 여기 두 상수로 모았다. 협의가 끝나면
     * 이 둘만 고치면 카운트다운·안내 문구가 같이 따라간다. */
    private static final DayOfWeek CHANGE_DEADLINE_DAY = DayOfWeek.THURSDAY;
    private static final LocalTime CHANGE_DEADLINE_TIME = LocalTime.of(18, 0);

    /* 배송은 주 1회 묶음. 마감 이후 첫 배송일이 그 주의 수령일이 된다. */
    private static final DayOfWeek DELIVERY_DAY = DayOfWeek.MONDAY;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일(E) a h시", Locale.KOREAN);
    private static final DateTimeFormatter DELIVERY_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN);

    // 구독 소개 — 비로그인도 볼 수 있어야 하는 유입 페이지
    @GetMapping("/subscribe")
    public String subscribePage() {
        return "subscribe";
    }

    // 내 구독 — 이번 주 메뉴를 바꾸는 화면. 로그인 필요(SecurityConfig 의 anyRequest 로 걸린다)
    @GetMapping("/subscription")
    public String mySubscriptionPage(Model model) {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime deadline = nextChangeDeadline(now);
        ZonedDateTime delivery = nextDelivery(deadline);

        // 카운트다운은 브라우저 시계로 돌아가므로 절대 시각(UTC ISO)을 넘긴다.
        model.addAttribute("changeDeadlineIso", deadline.toInstant().toString());
        model.addAttribute("changeDeadlineLabel", deadline.format(DEADLINE_FORMAT));
        model.addAttribute("deliveryLabel", delivery.format(DELIVERY_FORMAT));

        return "subscription";
    }

    /** 지금 기준으로 아직 지나지 않은 가장 가까운 변경 마감. */
    private static ZonedDateTime nextChangeDeadline(ZonedDateTime now) {
        ZonedDateTime candidate = now
                .with(TemporalAdjusters.nextOrSame(CHANGE_DEADLINE_DAY))
                .with(CHANGE_DEADLINE_TIME);
        // 마감 요일 당일이라도 시각이 지났으면 다음 주 마감이다.
        return candidate.isAfter(now) ? candidate : candidate.plusWeeks(1);
    }

    /** 그 마감으로 확정된 구성이 도착하는 날. */
    private static ZonedDateTime nextDelivery(ZonedDateTime deadline) {
        return deadline.with(TemporalAdjusters.next(DELIVERY_DAY));
    }
}

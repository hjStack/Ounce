package ounce.market.demo.subscription.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode {

    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    SUBSCRIPTION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "진행 중인 구독이 아닙니다."),
    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "이미 구독 중입니다."),

    INVALID_MEALS_PER_WEEK(HttpStatus.BAD_REQUEST, "주당 끼수는 4끼에서 7끼 사이여야 합니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "현재 상태에서는 처리할 수 없습니다."),

    CYCLE_NOT_FOUND(HttpStatus.NOT_FOUND, "회차 정보를 찾을 수 없습니다."),
    MENU_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "메뉴 선택 마감 시간이 지났습니다."),
    CYCLE_ALREADY_PAID(HttpStatus.CONFLICT, "이미 결제가 완료된 회차입니다."),

    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "구독 결제에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
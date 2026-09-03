package ounce.market.demo.subscription.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 구독 도메인 에러 코드.
 * <p>
 * 별도의 숫자 코드(SUB-001 등)를 두지 않고 enum 이름을 그대로 응답 코드로 쓴다.
 * 숫자 코드는 값을 볼 때마다 대조표를 열어야 하고, 항목을 지웠다가 번호를 재사용하는 순간
 * 클라이언트 분기가 조용히 깨진다. 이름은 로그에서도 응답에서도 그 자체로 읽힌다.
 * 대신 한 번 나간 이름은 리네임하지 않는다 — 이름이 곧 API 계약이다.
 */
@Getter
public enum SubscriptionErrorCode {

    // ===== 조회 실패 =====

    /** 구독이 없거나, 남의 구독이다. 소유권 위반도 404로 내려 존재 여부를 노출하지 않는다. */
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    CYCLE_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 회차 정보를 찾을 수 없습니다."),
    MENU_QUANTITY_MISMATCH(HttpStatus.BAD_REQUEST, "선택한 밀키트 수량이 구독 끼니 수와 맞지 않습니다."),

    /** 개별 항목의 수량이 1 미만이다. */
    INVALID_MENU_QUANTITY(HttpStatus.BAD_REQUEST, "밀키트 수량은 1끼 이상이어야 합니다."),

    /** 단가가 음수다. 상품 데이터가 깨졌다는 뜻이라 사용자 잘못이 아니다. */
    INVALID_PRODUCT_PRICE(HttpStatus.INTERNAL_SERVER_ERROR, "상품 정보를 불러오지 못했습니다."),
    // ===== 입력값 =====

    /** 구독 시작일은 오늘부터만 지정할 수 있다. */
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "구독 시작일은 오늘부터 선택할 수 있습니다."),

    /** 쉬어가기 종료일이 없거나 다음 결제일보다 이르다. */
    INVALID_RESUME_DATE(HttpStatus.BAD_REQUEST, "쉬어가기 종료일은 다음 결제일 이후로 선택해야 합니다."),

    /** 화면에서 4~7만 고를 수 있지만, API를 직접 때리는 요청을 막는다. */
    INVALID_MEALS_PER_WEEK(HttpStatus.BAD_REQUEST, "주당 끼니 수는 4끼에서 7끼 사이로 선택해야 합니다."),

    // ===== 상태 충돌 =====

    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "이미 진행 중인 구독이 있습니다."),

    SUBSCRIPTION_NOT_ACTIVE(HttpStatus.CONFLICT, "현재 상태에서는 변경할 수 없는 구독입니다."),

    /** PAUSED가 아닌데 재개, PAYMENT_RETRYING이 아닌데 즉시 재시도 같은 경우. */
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 구독 상태에서는 처리할 수 없는 요청입니다."),

    /** 결제일 23시 마감이 지나 이미 상품 준비가 시작됐다. */
    SKIP_DEADLINE_PASSED(HttpStatus.CONFLICT, "이번 주 변경 마감(결제일 23시)이 지나 쉬어갈 수 없습니다."),

    SKIP_ALREADY_SET(HttpStatus.CONFLICT, "이미 배송 1회 건너뛰기가 설정되어 있습니다."),
    SKIP_NOT_SET(HttpStatus.CONFLICT, "취소할 배송 건너뛰기가 없습니다."),

    /** 재시도 횟수를 다 썼거나 재시도 마감(다음날)이 지났다. */
    RETRY_DEADLINE_PASSED(HttpStatus.CONFLICT, "결제 재시도 가능 시간이 지났습니다. 다음 주 결제부터 적용됩니다."),

    /** 낙관적 락 충돌. 배치가 같은 구독을 결제 중일 때 사용자가 쉬어가기를 누른 경우. */
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "다른 처리가 진행 중입니다. 잠시 후 다시 시도해 주세요."),

    MENU_NOT_EDITABLE(HttpStatus.CONFLICT, "메뉴 변경 마감(결제일 23시)이 지났습니다."),
    MENU_NOT_SELECTED(HttpStatus.NO_CONTENT, "메뉴가 선택되지 않았습니다."),

    PRODUCT_NOT_AVAILABLE(HttpStatus.EARLY_HINTS,"상품이 존재하지 않습니다."),

    // ===== 결제 연동 =====

    PAYMENT_METHOD_NOT_REGISTERED(HttpStatus.CONFLICT, "등록된 결제 수단이 없습니다."),

    /** PG 자체 장애. 사용자 잘못이 아니므로 5xx로 내린다. */
    PAYMENT_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    PAYMENT_ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 시도 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;


    SubscriptionErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /** 응답 본문에 실리는 코드. 이름을 그대로 쓴다. */
    public String getCode() {
        return name();
    }
}

package ounce.market.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ounce.market.demo.common.ApiResponse;

@Slf4j // 로그를 찍기 위한 롬복 어노테이션
@RestControllerAdvice // 🔥 애플리케이션 전역의 에러를 낚아채는 핵심 어노테이션
public class GlobalExceptionHandler {

    /**
     * 1. 400 Bad Request: 비즈니스 로직에서 던지는 일반적인 예외들
     * (예: 아까 MemberService에서 이메일 중복일 때 던진 IllegalArgumentException)
     */

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad Request Exception: {}", e.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 2. 400 Bad Request: 커스텀 예외 처리
     * (예: 배송 지역 불가 에러 등, 나중에 BusinessException을 만들면 여기 추가!)
     */
    // @ExceptionHandler(OutOfDeliveryAreaException.class)
    // public ApiResponse<?> handleOutOfDeliveryAreaException(OutOfDeliveryAreaException e) { ... }

    /**
     * 3. 500 Internal Server Error: 최후의 방어막 (예상치 못한 서버 에러)
     * 개발자가 미처 처리하지 못한 NullPointerException 등이 여기서 잡힙니다.
     */

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("Internal Server Error: ", e); // 에러 로그는 무조건 남겨야 합니다!

        // 프론트엔드나 고객에게 서버 내부의 쌩(raw) 에러 코드를 보여주는 것은 보안상/UX상 좋지 않으므로 메세지를 숨깁니다.
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 일시적인 오류가 발생했습니다.");
    }
}

/**

 참고 (나중을 위한 최적화): 나중에는 GlobalExceptionHandler 안에 "없는 주소를 요청했을 때(404 Not Found)"를 따로 잡아내는 메서드를 하나 더 추가해서, "
 요청하신 페이지(API)를 찾을 수 없습니다."라고 응답하게 만들면 더 완벽해집니다!

 */
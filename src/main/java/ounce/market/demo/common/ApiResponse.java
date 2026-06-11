package ounce.market.demo.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // 데이터가 null일 경우 JSON 응답에서 필드 자체를 제외

@JsonPropertyOrder({"status", "message", "data"}) // 🔥 출력 순서를 강제로 고정!
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;

    // 1. 성공 응답 (기본 200 OK - 데이터가 있는 경우)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "SUCCESS", data);
    }

    // 2. 생성 성공 응답 (🔥 201 Created - 회원가입, 장바구니 담기 등에서 사용!)
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), message, data);
    }

    // 3. 성공 응답 (데이터가 없는 경우 - 예: 로그아웃, 게시글 삭제)
    // 메서드 이름을 분리하여 String 파라미터 충돌 방지
    public static <T> ApiResponse<T> successWithNoContent(String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, null);
    }

    // 4. 에러 응답 (GlobalExceptionHandler 등에서 사용)
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), message, null);
    }
}
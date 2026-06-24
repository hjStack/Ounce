package ounce.market.demo.common.global;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ounce.market.demo.common.dto.ErrorResponse;

// @RestControllerAdvice: 프로젝트 내의 모든 @RestController에서 발생하는 에러를 여기서 가로챕니다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 검증에 실패했을 때 Spring이 터뜨리는 예외를 잡는 메서드
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {

        // DTO에서 작성했던 여러 에러 메시지 중, 가장 첫 번째 에러 메시지만 쏙 뽑아옵니다.
        String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        // 우리가 만든 예쁜 에러 객체에 담습니다.
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);

        // HTTP 상태 코드 400(Bad Request)와 함께 프론트엔드로 반환!
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // (참고) 나중에 포인트 부족 등 커스텀 예외를 만들면 아래처럼 추가해 나가면 됩니다!
    /*
    @ExceptionHandler(InsufficientPointException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPoint(InsufficientPointException ex) {
        ErrorResponse response = new ErrorResponse(400, "포인트가 부족합니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    */
}
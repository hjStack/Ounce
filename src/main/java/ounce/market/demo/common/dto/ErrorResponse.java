package ounce.market.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;       // HTTP 상태 코드 (예: 400)
    private String message;   // 프론트엔드가 경고창(alert)에 띄워줄 친절한 메시지
}
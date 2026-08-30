package ounce.market.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;       // HTTP 상태 코드 (예: 400)
    private String message;
}
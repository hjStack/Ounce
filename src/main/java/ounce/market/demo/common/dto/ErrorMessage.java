package ounce.market.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ErrorMessage {

    public static final String DUPLICATE_EMAIL = "이미 사용 중인 이메일입니다.";
    public static final String MEMBER_NOT_FOUND = "존재하지 않는 회원입니다.";
}

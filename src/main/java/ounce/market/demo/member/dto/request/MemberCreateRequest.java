package ounce.market.demo.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import ounce.market.demo.member.entity.Role;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class MemberCreateRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 8~20자리수어야 하며, 영문/숫자/특수문자를 모두 포함해야 합니다.")
    private String password;

    private String name;

    private Role role;

}

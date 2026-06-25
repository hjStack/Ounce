package ounce.market.demo.member.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.common.Exception.DuplicateEmailException;
import ounce.market.demo.common.dto.ErrorMessage;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.dto.request.LoginRequest;
import ounce.market.demo.member.dto.request.MemberCreateRequest;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.entity.Role;
import ounce.market.demo.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    @Transactional
    public void signup(MemberCreateRequest request) {
        // 1. 이메일 중복 검사 로직 (중복 시 예외 발생)

        if (memberRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmailException(ErrorMessage.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화 로직
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Member 엔티티 빌드 (Builder 패턴 사용)
        Member member = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .point(0) // (포인트 같은 초기값도 챙겨주시면 좋습니다)
                .role(Role.USER)
                .build();

        // 4. DB에 저장 (memberRepository.save)
        memberRepository.save(member) ;
    }


    // 로그인 로직 추가
    @Transactional
    public String login(LoginRequest request) {

        // 권한 검증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. 검증을 무사히 통과하면 JWTUtil을 불러 Access Token을 발급합니다!
        // (원래는 Refresh Token도 여기서 같이 발급해야 합니다)
        return jwtUtil.createAccessToken(request.getEmail(), "ROLE_USER");
    }

}

package ounce.market.demo.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.dto.request.LoginRequest;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    // 1. 로그인 요청이 들어오면 가로채는 메서드
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ObjectMapper om = new ObjectMapper();
            LoginRequest loginRequest = om.readValue(request.getInputStream(), LoginRequest.class);

            String email = loginRequest.getEmail();
            String password = loginRequest.getPassword();

//            log.info("로그인 시도 이메일: {}", email);

            // 토큰을 만들어서 보안 팀장(AuthenticationManager)에게 검증을 맡김
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);
            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 2. 로그인 성공 시 실행되는 메서드 (여기서 JWT 발급!)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        // 인증된 유저 정보 꺼내기
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = customUserDetails.getUsername();

        // 권한 꺼내기 (ROLE_USER)
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // JWT 발급
        String token = jwtUtil.createAccessToken(email, role);

        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("Authorization", token)
                .path("/")
                .httpOnly(true)
                // .secure(true) // 로컬 테스트 시 주석
                .maxAge(60 * 60 * 24)
                .sameSite("Lax")
                .build();


        // 🔥 RFC 7235 규약에 따라 헤더에 Bearer 붙여서 응답
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"로그인 성공!\"}");

        log.info("로그인 성공 및 JWT 발급 완료: {}", email);
    }

    // 3. 로그인 실패 시 실행되는 메서드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        // 🔥 실패 시 401 응답코드 전송
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"로그인 실패: 이메일이나 비밀번호를 확인해주세요.\"}");

        log.info("로그인 실패");
    }
}
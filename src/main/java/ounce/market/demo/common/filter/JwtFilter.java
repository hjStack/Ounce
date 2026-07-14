package ounce.market.demo.common.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.entity.Member;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = null;

        // 1. 클라이언트가 보낸 쿠키통(Cookies)에서 "Authorization" 쿠키 찾기
        Cookie[] cookies = request.getCookies();

        if (cookies == null){
            log.debug("⚠️ 쿠키 없음! 요청 URI: {}", request.getRequestURI());  // 👈 URI 추가
        }

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. 토큰이 아예 없거나, 검증(validateToken)에 실패하면 통과 안 시킴!
        if (token == null || !jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);  // 여기서 401
            return;
        }

        // 3. 토큰이 유효하다면, 내부에 숨겨진 이메일과 권한(Role) 꺼내기
        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);

        Member temporaryMember = Member.builder()
                .email(email)
                .role(ounce.market.demo.member.entity.Role.valueOf(role.replace("ROLE_", "")))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(temporaryMember);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                userDetails, // 💡 기존 email 문자열에서 userDetails 객체로 변경!
                null,
                userDetails.getAuthorities()
        );

        // 5. 💡 핵심: "이 유저 인증 통과했어!" 라고 스프링 시큐리티 상황실(SecurityContext)에 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 6. 무사히 인증을 마쳤으니 다음 필터로(또는 실제 컨트롤러로) 이동!
        filterChain.doFilter(request, response);
    }
}

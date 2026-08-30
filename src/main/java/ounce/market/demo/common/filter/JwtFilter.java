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
import ounce.market.demo.member.entity.MemberStatus;
import ounce.market.demo.member.entity.Role;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 토큰 없음 → 익명으로 통과 (인가 단계에서 처리)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Access Token 검증 (타입까지 확인)
        if (!jwtUtil.validateAccessToken(token)) {
            log.debug("유효하지 않은 access token. URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);

        // 방어 코드: claim이 비어있으면 인증하지 않음
        if (email == null || role == null) {
            log.warn("토큰에 필수 claim 누락. URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Role parsedRole;
        try {
            parsedRole = Role.valueOf(role.replace("ROLE_", ""));
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 role 값: {}", role);
            filterChain.doFilter(request, response);
            return;
        }

        Member temporaryMember = Member.builder()
                .email(email)
                .role(parsedRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(temporaryMember);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

}

package ounce.market.demo.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import ounce.market.demo.common.global.CustomUserDetailsService;
import ounce.market.demo.common.global.jwt.JWTUtil;

import java.io.IOException;

/**
 * 쿠키의 access token으로 인증 주체를 세운다.
 * <p>
 * 토큰 claim만으로 Member를 조립하지 않고 DB에서 조회한다. 이유가 둘이다.
 * 하나는 memberId가 필요해서다 — 조립한 객체는 ID가 비어 있어 구독·주문처럼
 * 회원 ID로 조회하는 모든 경로가 깨진다.
 * 다른 하나는 권한이다. 토큰의 role은 발급 시점 값이라, 관리자 권한을 회수해도
 * 토큰이 만료될 때까지 관리자로 통과한다. DB에서 읽으면 즉시 반영된다.
 * <p>
 * 대가는 요청당 회원 조회 한 번이다. 이게 부담이 되면 토큰에 memberId를 담는 방식으로
 * 바꿀 수 있지만, 그때는 권한 즉시 반영을 포기하는 것이다.
 */


@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    // access-token
//    private static final String TOKEN_COOKIE_NAME = "__Host-oz-a";

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final String accessCookieName;

    public JwtFilter(
            JWTUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            @Value("${app.cookie.access-name}") String accessCookieName
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.accessCookieName = accessCookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        // 토큰 없음 → 익명으로 통과. 접근 제어는 SecurityFilterChain이 한다.
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.validateAccessToken(token)) {
            log.debug("유효하지 않은 access token. URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.getEmail(token);
        if (email == null) {
            log.warn("토큰에 email claim 누락. URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails;
        try {
            // 권한도 여기서 나온다. CustomUserDetails.getAuthorities()가
            // DB에서 읽은 member.getRole()로 SimpleGrantedAuthority를 만든다.
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            // 토큰은 유효한데 회원이 없다. 탈퇴했거나 데이터가 지워진 경우다.
            log.warn("토큰의 회원을 찾을 수 없음. email={} URI={}", email, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (accessCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
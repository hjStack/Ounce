package ounce.market.demo.member.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.entity.Role;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.cookie.access-name}") private String accessCookieName;
    @Value("${app.cookie.refresh-name}") private String refreshCookieName;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    public void issue(String email, HttpServletResponse response, String role) {
        String accessToken = jwtUtil.createAccessToken(email,role);
        String refreshToken = jwtUtil.createRefreshToken(email);

        redisTemplate.opsForValue()
                .set("refresh:" + email, refreshToken, 14, TimeUnit.DAYS);

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(accessCookieName, accessToken)
                        .path("/").httpOnly(true).maxAge(Duration.ofMinutes(30))
                        .sameSite("Lax").secure(cookieSecure).build().toString());

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(refreshCookieName, refreshToken)
                        .path("/api/auth/refresh").httpOnly(true).maxAge(Duration.ofDays(14))
                        .sameSite("Lax").secure(cookieSecure).build().toString());
    }

}

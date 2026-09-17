package ounce.market.demo.common.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;

@Tag(name = "01-1 리프레시 토큰",description = "refresh token 생성 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.access-name}")
    private String accessCookieName;

    @Value("${app.cookie.refresh-name}")
    private String refreshCookieName;


    // refresh
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {

        String refreshToken = extractCookie(request, refreshCookieName);

        // 1. 쿠키 존재 + 서명/만료/타입 검증
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtUtil.getEmail(refreshToken);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        // 2. Redis에 저장된 토큰과 일치하는지 확인
        String stored = redisTemplate.opsForValue().get("refresh:" + email);
        if (stored == null || !stored.equals(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        // 3. 현재 권한을 DB에서 조회 (탈퇴/권한변경 반영)
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // 4. 새 access token 발급
        String newAccessToken = jwtUtil.createAccessToken(email, member.getRole().name());

        ResponseCookie cookie = ResponseCookie.from(accessCookieName, newAccessToken)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .maxAge(60 * 30)
                .sameSite("Lax")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
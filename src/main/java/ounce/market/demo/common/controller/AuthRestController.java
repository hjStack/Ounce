package ounce.market.demo.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;

@Hidden
@RestController
@RequiredArgsConstructor
public class AuthRestController {

    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "Refresh", required = false) String refreshToken,
            HttpServletResponse response) {

        // 1. refresh 쿠키 자체가 없으면
        if (refreshToken == null) {
            return ResponseEntity.status(401).body("refresh 토큰 없음. 재로그인 필요");
        }

        // 2. refresh token 서명·만료 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("refresh 만료. 재로그인 필요");
        }

        // 3. refresh token에서 email 꺼내기 (subject에 저장했으므로 getSubject)
        String email = jwtUtil.getClaims(refreshToken).getSubject();

        // 4. Redis에 저장된 refresh랑 일치하는지 (핵심 — 무효화 검증)
        String stored = redisTemplate.opsForValue().get("refresh:" + email);

        if (stored == null || !stored.equals(refreshToken)) {
            return ResponseEntity.status(401).body("무효한 refresh");
        }

        // 5. 실제 role 조회 (하드코딩 대신 DB에서)
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        String role = member.getRole().name();

        // 6. 새 access token 발급
        String newAccess = jwtUtil.createAccessToken(email, role);
        ResponseCookie accessCookie = ResponseCookie.from("Authorization", newAccess)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(60 * 30)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok().build();
    }
}
package ounce.market.demo.common.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil {

    private final SecretKey secretKey;
    private final Long expirationTime;
    private final Long refreshExpirationTime;

    public JWTUtil(@Value("${spring.jwt.secret}") String secret,
                   @Value("${spring.jwt.expiration_time}") Long expirationTime,
                   @Value("${spring.jwt.refresh_expiration_time}") Long refreshExpirationTime){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
        this.refreshExpirationTime=refreshExpirationTime;
    }

    // 1. Access Token 발급 (이메일 기반 로그인 검증용)
    public String createAccessToken(String email, String role) {
        return Jwts.builder()
                .claim("email", email) // 토큰 안에 유저 이메일(식별자) 저장
                .claim("role", role)   // 토큰 안에 유저 권한(ROLE_USER 등) 저장
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact(); // 토큰 압축 및 생성
    }

    // 2. Refresh Token 발급
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationTime);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 3. 토큰에서 이메일(Subject) 꺼내기
    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // 4. 토큰에서 권한 꺼내기
    public String getRole(String token){
        return getClaims(token).get("role", String.class);
    }

    // 5. 남은 만료 시간 계산
    public long getRemainingTime(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            long now = new Date().getTime();
            return expiration.getTime() - now;
        } catch (Exception e) {
            return 0;
        }
    }


    // 6.payload 꺼내기
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    // 7.토큰 유효기간 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
             log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
             log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
             log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
             log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }



}

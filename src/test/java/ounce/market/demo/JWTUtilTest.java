package ounce.market.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ounce.market.demo.common.global.jwt.JWTUtil;

import static org.assertj.core.api.Assertions.assertThat;

class JWTUtilTest {

    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // HS256은 최소 32바이트 키 필요
        jwtUtil = new JWTUtil(
                "test-secret-key-for-unit-test-1234567890",
                1000L * 60 * 30,          // access 30분
                1000L * 60 * 60 * 24 * 14 // refresh 2주
        );
    }

    @Test
    void refresh토큰은_access자리에서_거부된다() {
        String refresh = jwtUtil.createRefreshToken("test@test.com");
        assertThat(jwtUtil.validateAccessToken(refresh)).isFalse();
    }

    @Test
    void access토큰은_refresh자리에서_거부된다() {
        String access = jwtUtil.createAccessToken("test@test.com", "USER");
        assertThat(jwtUtil.validateRefreshToken(access)).isFalse();
    }

    @Test
    void 정상토큰은_각자_자리에서_통과한다() {
        assertThat(jwtUtil.validateAccessToken(
                jwtUtil.createAccessToken("test@test.com", "USER"))).isTrue();
        assertThat(jwtUtil.validateRefreshToken(
                jwtUtil.createRefreshToken("test@test.com"))).isTrue();
    }
}
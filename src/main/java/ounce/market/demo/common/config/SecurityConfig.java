package ounce.market.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 💥 핵심: BCrypt 암호화 기계를 Spring의 Bean(공용 객체)으로 등록합니다!
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 🚦 방화벽 규칙 설정 (누구는 통과시키고, 누구는 막을 것인가?)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 우리는 프론트엔드 통신(fetch)을 쓰므로 CSRF 방어막은 일단 끕니다.
                .csrf(csrf -> csrf.disable())

                // 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 1. 프론트엔드 디자인 파일들 (css, js, img) 통과!
                        .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/signup", "/login").permitAll()
                        .requestMatchers("/api/members/**").permitAll()

                        .requestMatchers("/error").permitAll()

                        // 4. 그 외에 나중에 만들 상품 조회, 주문 API 등은 전부 인증(로그인) 필요!
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
package ounce.market.demo.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ounce.market.demo.common.OAuth.CustomOAuth2UserService;
import ounce.market.demo.common.OAuth.OAuth2SuccessHandler;
import ounce.market.demo.common.filter.JwtFilter;
import ounce.market.demo.common.global.jwt.JWTUtil;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler; // 우리가 직접 만들 클래스
    private final CustomOAuth2UserService  customOAuth2UserService; // 구글 정보 처리 클래스
    private final JWTUtil jwtUtil;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 프론트엔드 디자인 파일들 (css, js, img) 통과!
                        .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/signup", "/login").permitAll()
                        .requestMatchers("/api/members/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()

                        .requestMatchers("/error").permitAll()

                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService) // Step 1: 구글에서 사용자 이메일, 이름 가져오기
                )
                .successHandler(oAuth2SuccessHandler) // Step 2: 정보 가져오기 성공하면 JWT 만들어서 프론트로 던져주기!
        )
                .addFilterBefore(new JwtFilter(jwtUtil),UsernamePasswordAuthenticationFilter.class);  // 응답 헤더에 쿠키를 심고 메인으로 리다이렉팅

        return http.build();
    }
}


/**
 세션은 서버에 로그인 상태를 저정하는 Stateful 방식이라 MSA에서 쓰기 어려움
 반면 JWT는 서버가 아무것도 기억하지 않는 Stateless 방식이라 MSA 환경의 표준으로 사용함
 */
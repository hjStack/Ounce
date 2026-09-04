package ounce.market.demo.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import ounce.market.demo.common.global.CustomUserDetailsService;
import ounce.market.demo.common.global.jwt.JWTUtil;
import org.springframework.http.HttpMethod;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler; // 우리가 직접 만들 클래스
    private final CustomOAuth2UserService  customOAuth2UserService; // 구글 정보 처리 클래스
    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.cookie.access-name}")
    private String accessCookieName;


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


                        .requestMatchers("/css/**", "/js/**", "/img/**","/favicon.ico","/favicon.png").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/signup", "/login").permitAll()
                        .requestMatchers("/api/members/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        // 관리자 화면(/admin/qna 등)도 API 와 같은 권한으로 막는다.
                        // 안 적으면 anyRequest 로 떨어져서 로그인한 일반 회원도 화면이 열린다.
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/products/search").permitAll()
                        .requestMatchers("/dev/**").permitAll()
                        .requestMatchers("/timedeal").permitAll()   // 미드나이트 페이지 자체도 열기
                        // 반면 /subscription(내 구독)은 여기 안 적어서 anyRequest 로 로그인이 걸린다.
                        .requestMatchers("/subscribe").permitAll()
                        // 고객센터: FAQ 는 누구나 봐야 한다. 문의 API(/api/qna/**)만 anyRequest 로 로그인이 걸린다.
                        .requestMatchers("/support").permitAll()
                        .requestMatchers("/api/products/**", "/products/**", "/products-detail/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/timedeal").permitAll()          // 조회는 누구나
                        .requestMatchers("/api/timedeal/purchase/**").authenticated()         // 구매는 로그인
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/policy.html").permitAll()
                        .requestMatchers("/terms.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/*/reviews").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()
                        .requestMatchers("/api/carts/**").authenticated()
                        .requestMatchers("/api/coupons/**").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )



                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")

                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService) // Step 1: 구글에서 사용자 이메일, 이름 가져오기
                )
                .successHandler(oAuth2SuccessHandler) // Step 2: 정보 가져오기 성공하면 JWT 만들어서 프론트로 던져주기!
        )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            if (req.getRequestURI().startsWith("/api/")) {
                                res.setStatus(401);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write("{\"message\":\"인증이 필요합니다\"}");
                            } else {
                                res.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"message\":\"권한이 없습니다\"}");
                        })
                )
                .addFilterBefore(new JwtFilter(jwtUtil, customUserDetailsService,accessCookieName),UsernamePasswordAuthenticationFilter.class);  // 응답 헤더에 쿠키를 심고 메인으로 리다이렉팅

        return http.build();
    }
}


/**
 세션은 서버에 로그인 상태를 저정하는 Stateful 방식이라 MSA에서 쓰기 어려움
 반면 JWT는 서버가 아무것도 기억하지 않는 Stateless 방식이라 MSA 환경의 표준으로 사용함
 */

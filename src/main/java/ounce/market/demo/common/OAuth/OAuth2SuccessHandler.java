package ounce.market.demo.common.OAuth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ounce.market.demo.common.global.jwt.JWTUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    // 💡 application.yml에 적힌 주소를 자동으로 읽어옵니다.
    @Value("${app.frontend-url}")
    private String frontendUrl;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 2. 해당 이메일로 JWT (Access Token) 생성
        String token = jwtUtil.createAccessToken(email, role);

        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(60 * 60 * 24)
                .sameSite("Lax")
                .build();

        // 4. 응답 헤더에 쿠키 주입
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 4. 프론트엔드로 리다이렉트 (이동)
       response.sendRedirect(frontendUrl);
    }
}

/**
 * 쿠키란
 쿠키는 한마디로 "서버가 유저의 웹 브라우저(크롬, 사파리 등)에 몰래 쥐여주는 아주 작은 텍스트 메모지"입니다.

 인터넷 통신(HTTP)은 본래 '무상태(Stateless)'입니다.
 즉, 브라우저가 서버에 요청을 보내고 응답을 받으면 통신이 끊어지고, 서버는 방금 자기가 누구랑 대화했는지 1초 만에 까먹어버립니다. (완벽한 기억 상실증이죠.)

 발급: 유저가 로그인을 성공하면, 서버는 "이 사람은 인증된 유저임. 토큰 값: 12345" 라고 적힌 쿠키(메모지)를 만들어서 프론트엔드로 던져줍니다.
 보관: 웹 브라우저는 이 쿠키를 받으면 군말 없이 자기 컴퓨터(또는 스마트폰) 내부의 전용 보관소에 고이 저장해 둡니다.
 자동 제출: 이후 유저가 Ounce 쇼핑몰의 메인 페이지를 누르든, 장바구니를 누르든, 브라우저는 서버에 요청을 보낼 때마다 알아서 그 쿠키를 찾아내어 요청(Request)에 꼬리표처럼 찰싹 붙여서 보냅니다.
 인식: 서버는 요청에 달린 쿠키를 보고 "아! 아까 인증해 줬던 그 유저구나!" 하고 기억을 되살려 서비스를 제공합니다.
 */
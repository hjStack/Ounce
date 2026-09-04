package ounce.market.demo.member.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.RollbackException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.dto.request.MemberCreateRequest;
import ounce.market.demo.member.dto.request.LoginRequest;
import ounce.market.demo.member.dto.response.MemberResponse;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.entity.Role;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.member.service.AuthTokenService;
import ounce.market.demo.member.service.MemberService;
import ounce.market.demo.order.entity.OrderStatus;
import ounce.market.demo.order.repository.OrderRepository;

import java.util.concurrent.TimeUnit;
@Tag(name = "01. 회원", description = "회원가입, 로그인, 로그아웃")
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

     private final MemberService memberService;
     private final OrderRepository orderRepository;
     private final RedisTemplate<String,String> redisTemplate;

     private final JWTUtil jwtUtil;
     private final AuthTokenService  authTokenService;

    @Value("${app.cookie.access-name}")
    private String accessCookieName;

    @Value("${app.cookie.refresh-name}")
    private String refreshCookieName;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody MemberCreateRequest request, HttpServletResponse response) {
        // @Valid를 통과했다면 이곳의 코드가 실행됩니다!
        Member member = memberService.signup(request);

        String email = request.getEmail();
        String refreshToken = jwtUtil.createRefreshToken(email);
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, 14, TimeUnit.DAYS);

        authTokenService.issue(email, response, "ROLE_" + member.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        String token = memberService.login(request);

        // access token
        ResponseCookie cookie = ResponseCookie.from(accessCookieName, token)
                .path("/")
                .httpOnly(true)
                .maxAge(60 * 30)  // 30분
                .sameSite("Lax")
                .secure(false)
                .build();

        String email = request.getEmail();
        String refreshToken = jwtUtil.createRefreshToken(email);
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, 14, TimeUnit.DAYS);

        // refresh token
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .path("/api/auth/refresh")
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 14)  // 2주
                .sameSite("Lax")
                .secure(false)
                .build();

//        authTokenService.issue(email, response, "ROLE_" + );

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());


        return ResponseEntity.ok()
                .body("로그인 성공! 쿠키를 확인하세요.");
    }

    // 2. 💡 내 정보 조회 API
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // JwtFilter를 통과하지 못해 Authentication이 없다면 401 에러 반환
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = userDetails.getUsername();
        Member member = memberService.findByEmail(email);

        long totalSpent = orderRepository.sumTotalAmountByMemberId(
                member.getMemberId(), OrderStatus.PAYMENT_COMPLETED);

        // 등급 구분
        // todo 등급 가격을 다시 정해야할것같음
        // 50만원 이상 VIP
        // 10만원 이상 GOLD
        String grade = totalSpent >= 500_000 ? "VIP"
                : totalSpent >= 100_000 ? "GOLD"
                : "BASIC";

        // 3. 완전한 엔티티를 DTO로 변환하여 응답합니다.
        MemberResponse response = MemberResponse.from(member,grade);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Redis에서 refresh 삭제 (무효화)
        if (userDetails != null) {
            redisTemplate.delete("refresh:" + userDetails.getUsername());
        }

        // access 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from(accessCookieName, "")
                .path("/").httpOnly(true).maxAge(0).sameSite("Lax").build();

        // refresh 쿠키도 삭제
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, "")
                .path("/api/auth/refresh").httpOnly(true).maxAge(0).sameSite("Lax").build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok("로그아웃 성공");
    }

    // 멤버 수 세는 api -> 없어도 될것같음
//    @GetMapping("/count")
//    public ResponseEntity<Long> getMemberCount() {
//        return ResponseEntity.ok(memberRepository.count());   // JpaRepository 기본 제공
//    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) {

        String email = userDetails.member().getEmail();
        memberService.deleteMember(email);

        // Redis refresh 토큰도 제거
        redisTemplate.delete("refresh:" + email);

        // Authorization 쿠키 만료
        ResponseCookie accessCookie = ResponseCookie.from(accessCookieName, "")
                .path("/").httpOnly(true).maxAge(0).sameSite("Lax").build();

        // Refresh 쿠키 만료
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, "")
                .path("/api/auth/refresh").httpOnly(true).maxAge(0).sameSite("Lax").build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.noContent().build();
    }

}
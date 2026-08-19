package ounce.market.demo.member.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
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
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.member.service.MemberService;
import ounce.market.demo.order.entity.OrderStatus;
import ounce.market.demo.order.repository.OrderRepository;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

     private final MemberService memberService; // 나중에 서비스 연결
     private final MemberRepository memberRepository;
     private final OrderRepository orderRepository;
     private final RedisTemplate<String,String> redisTemplate;

     private final JWTUtil jwtUtil;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody MemberCreateRequest request) {
        // @Valid를 통과했다면 이곳의 코드가 실행됩니다!
         memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        String token = memberService.login(request);

        // access token
        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .path("/")
                .httpOnly(true)
                .maxAge(60 * 30)
                .sameSite("Lax")
                .secure(false)
                .build();

        String email = request.getEmail();
        String refreshToken = jwtUtil.createRefreshToken(email);
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, 14, TimeUnit.DAYS);

        ResponseCookie refreshCookie = ResponseCookie.from("Refresh", refreshToken)
                .path("/api/auth/refresh")
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 14)
                .sameSite("Lax")
                .secure(false)
                .build();

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
        ResponseCookie cookie = ResponseCookie.from("Authorization", "")
                .path("/").httpOnly(true).maxAge(0).sameSite("Lax").build();
        // refresh 쿠키도 삭제
        ResponseCookie refreshCookie = ResponseCookie.from("Refresh", "")
                .path("/api/auth/refresh").httpOnly(true).maxAge(0).sameSite("Lax").build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok("로그아웃 성공");
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getMemberCount() {
        return ResponseEntity.ok(memberRepository.count());   // JpaRepository 기본 제공
    }

}
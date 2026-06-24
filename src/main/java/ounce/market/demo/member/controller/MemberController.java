package ounce.market.demo.member.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.member.dto.request.MemberCreateRequest;
import ounce.market.demo.member.dto.request.LoginRequest;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    // private final MemberService memberService; // 나중에 서비스 연결

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody MemberCreateRequest request) {
        // @Valid를 통과했다면 이곳의 코드가 실행됩니다!
//         memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // memberService.login(request);
        return ResponseEntity.ok("검증 통과! 로그인 로직 실행");
    }
}
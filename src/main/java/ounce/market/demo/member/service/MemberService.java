package ounce.market.demo.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import ounce.market.demo.cart.entity.Cart;
import ounce.market.demo.cart.repository.CartProductRepository;
import ounce.market.demo.cart.repository.CartRepository;
import ounce.market.demo.common.Exception.DuplicateEmailException;
import ounce.market.demo.common.Exception.MemberNotFoundException;
import ounce.market.demo.common.dto.ErrorMessage;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.dto.request.LoginRequest;
import ounce.market.demo.member.dto.request.MemberCreateRequest;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.entity.MemberStatus;
import ounce.market.demo.member.entity.Role;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.review.repository.ReviewRepository;

import java.util.Collections;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final CartRepository cartRepository; // 💡 장바구니 창고 직원 추가
    private final CartProductRepository cartProductRepository;
    private final ReviewRepository reviewRepository;
    private final RedisTemplate redisTemplate;

    // todo 관리자 권한 로직 및 버튼 만들기
    // todo n+1 문제 발생 -> 해결하기

    @Transactional
    public Member signup(MemberCreateRequest request) {

        validateRejoinAllowed(request.getEmail());

        // 1. 이메일 중복 검사 로직 (중복 시 예외 발생)
        if (memberRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmailException(ErrorMessage.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화 로직
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Member 엔티티 빌드 (Builder 패턴 사용)
        Member member = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .point(1000)   // 가입 축하금 1000원
                .role(Role.USER)
                .build();

        // 4. DB에 저장 (memberRepository.save)
        Member savedMember = memberRepository.save(member);

        // 회원가입할때 카트 생성
        Cart newCart = Cart.builder().member(savedMember).build();
        cartRepository.save(newCart);

        return savedMember;
    }

    public void validateRejoinAllowed(String email) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        if (memberRepository.findByWithdrawnEmailAndDeletedAtAfter(
                email, thirtyDaysAgo).isPresent()) {
            throw new DuplicateEmailException(
                    "탈퇴한 계정은 탈퇴 후 30일 동안 다시 가입할 수 없습니다.");
        }
    }

    // 로그인 로직 추가
    @Transactional
    public String login(LoginRequest request) {

        // 인증 전에 회원 상태 먼저 확인
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }

        // 권한 검증
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }


        // 2. 검증을 무사히 통과하면 JWTUtil을 불러 Access Token을 발급합니다!
        // (원래는 Refresh Token도 여기서 같이 발급해야 합니다)

        String accessToken = jwtUtil.createAccessToken(
                request.getEmail(),
                "ROLE_" + member.getRole().name()
        );

        return accessToken;
    }

    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        // Repository에서 Optional로 반환한다고 가정했을 때의 안전한 코드
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저를 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteMember(String email) {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFoundException(email));

        cartRepository.findByMemberMemberId(member.getMemberId())
                .ifPresent(cart -> {
                    cartProductRepository.deleteByCart_CartId(cart.getCartId());
                    reviewRepository.findByMemberIdWithMember(member.getMemberId());
                    cartRepository.delete(cart);
                });

        member.withdraw();
        redisTemplate.delete("refresh:" +email);
    }

}

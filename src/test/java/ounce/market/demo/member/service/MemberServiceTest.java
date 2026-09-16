package ounce.market.demo.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import ounce.market.demo.cart.entity.Cart;
import ounce.market.demo.cart.repository.CartRepository;
import ounce.market.demo.common.Exception.DuplicateEmailException;
import ounce.market.demo.common.global.jwt.JWTUtil;
import ounce.market.demo.member.dto.request.MemberCreateRequest;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.entity.MemberStatus;
import ounce.market.demo.member.entity.Role;
import ounce.market.demo.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CartRepository cartRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtil jwtUtil;

    @Test
    @DisplayName("회원가입 성공: 회원 정보와 장바구니가 정상적으로 저장되어야 한다.")
    void memberSignup(){

        // given
        MemberCreateRequest createRequest = new MemberCreateRequest("test@test.com", "Mickey5172222", "HJ",Role.USER);
        //when
        given(memberRepository.existsByEmail(createRequest.getEmail())).willReturn(false);
        given(passwordEncoder.encode(createRequest.getPassword())).willReturn("encodedPassword");

        memberService.signup(createRequest);


        //then
        verify(memberRepository).existsByEmail(createRequest.getEmail());
        verify(passwordEncoder).encode(createRequest.getPassword());
        verify(memberRepository).save(any(Member.class));
        verify(cartRepository).save(any(Cart.class)); // 장바구니 생성 로직도 탔는지 검증!
    }

    @Test
    @DisplayName("회원가입 실패: 이미 존재하는 이메일이면 DuplicateEmailException이 발생해야 한다.")
    void signup_Fail_DuplicateEmail() {
        // given
        MemberCreateRequest request = new MemberCreateRequest("duplicate@test.com", "password", "중복유저",Role.USER);

        // 이메일이 이미 존재한다고 가짜 대본을 줍니다.
        given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then
        assertThrows(DuplicateEmailException.class, () -> memberService.signup(request));

        // 예외가 터졌으니, 비밀번호 암호화나 저장은 1번도 실행되지 않아야 합니다.
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(anyString());
        verify(memberRepository, org.mockito.Mockito.never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패: 탈퇴 후 30일 이내에는 동일 이메일로 재가입할 수 없어야 한다.")
    void signup_Fail_WithinThirtyDaysAfterWithdrawal() {
        Member withdrawnMember = Member.builder()
                .email("withdrawn_1@ounce.deleted")
                .withdrawnEmail("withdrawn@test.com")
                .status(MemberStatus.WITHDRAWN)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .build();
        MemberCreateRequest request = new MemberCreateRequest(
                "withdrawn@test.com", "password", "탈퇴회원", Role.USER);

        given(memberRepository.findByWithdrawnEmailAndDeletedAtAfter(
                anyString(), any(LocalDateTime.class)))
                .willReturn(Optional.of(withdrawnMember));

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> memberService.signup(request));

        assertEquals(
                "탈퇴한 계정은 탈퇴 후 30일 동안 다시 가입할 수 없습니다.",
                exception.getMessage());
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(anyString());
        verify(memberRepository, org.mockito.Mockito.never()).save(any(Member.class));
    }

//    @Test
//    @DisplayName("로그인 성공: 권한 검증 후 JWT 토큰을 반환해야 한다.")
//    void login_Success() {
//        // given
//        LoginRequest request = new LoginRequest("test@test.com", "password");
//        Authentication mockAuthentication = mock(Authentication.class);
//        String expectedToken = "mock.jwt.token";
//
//        // AuthenticationManager가 무사히 검증을 통과했다고 가정
//        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .willReturn(mockAuthentication);
//        // 토큰 생성 시 예상되는 가짜 토큰 반환
//        given(jwtUtil.createAccessToken(request.getEmail(), "ROLE_USER"))
//                .willReturn(expectedToken);
//
//        // when
//        String actualToken = memberService.login(request);
//
//        // then
//        assertEquals(expectedToken, actualToken);
//        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(jwtUtil).createAccessToken(request.getEmail(), "ROLE_USER");
//    }

    @Test
    @DisplayName("이메일로 회원 찾기 성공")
    void findByEmail_Success() {
        // given
        String email = "test@test.com";
        Member mockMember = Member.builder().email(email).build();
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(mockMember));

        // when
        Member foundMember = memberService.findByEmail(email);

        // then
        assertEquals(email, foundMember.getEmail());
    }
}

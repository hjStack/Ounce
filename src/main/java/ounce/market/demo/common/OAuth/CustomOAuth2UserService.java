package ounce.market.demo.common.OAuth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ounce.market.demo.member.entity.Role;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// 5. 유저 정보 조회 — CustomOAuth2UserService

// todo DefaultOAuth2UserService  ⭐️
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    /** todo
      구글 로그인을 하면 자꾸 로컬로 url이 바뀌는 오류 발견
     */

    // ⭐️
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 구글에서 유저 정보 가져오기 (기본 기능 호출)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 구글이 던져준 정보(Attributes) 중에서 이메일  추출
        String email = oAuth2User.getAttribute("email");

        // 3. 우리 DB에 이 이메일이 있는지 확인하고, 없으면 회원가입(저장) 처리
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 구글 로그인 유저는 비밀번호가 없으므로 UUID로 임의 생성 후 암호화
                    String password = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

                    Member newMember = Member.builder()
                            .email(email)
                            .password(password)
                            .role(Role.USER)
                            .point(0) // 초기 포인트 설정
                            .build();
                    return memberRepository.save(newMember);
                });

        // 4. Spring Security가 알아먹을 수 있는 객체로 포장해서 반환
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().name())),
                oAuth2User.getAttributes(),
                userNameAttributeName // 구글의 경우 자동으로 "sub"가 들어갑니다.
        );
    }
}
package ounce.market.demo.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.entity.Role;
import ounce.market.demo.member.repository.MemberRepository;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // "admin@ounce.com" 이라는 계정이 DB에 없을 때만 실행
        if (!memberRepository.existsByEmail("hye_jun0209@icloud.com")) {

            // 본인의 Member 엔티티 구조에 맞게 수정해주세요!
            Member admin = Member.builder()
                    .email("hye_jun0209@icloud.com")
                    .password(passwordEncoder.encode("Mickey51799@#@")) // 💡 암호화 필수!
                    .name("관리자 : hj")
                    .role(Role.ADMIN) // 💡 열거형(Enum) 등에 정의된 관리자 권한
                    .build();

            memberRepository.save(admin);
            System.out.println("✅ 관리자 계정이 생성되었습니다. ");
        }
    }
}
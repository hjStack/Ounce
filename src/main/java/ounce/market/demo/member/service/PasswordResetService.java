package ounce.market.demo.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.MailSender;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final String TOKEN_PREFIX = "pwreset:";

    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void requestReset(String email) {
        memberRepository.findByEmail(email)
                .filter(this::canResetPassword)
                .ifPresent(this::sendResetMail);
        // 회원이 없어도 예외 없이 정상 종료
    }

    private boolean canResetPassword(Member member) {
        // 탈퇴 회원 제외, 소셜 가입 회원 제외
        return member.getEmail() != null;
    }

    private void sendResetMail(Member member) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue()
                .set(TOKEN_PREFIX + token, member.getEmail(), TOKEN_TTL);

        String link = frontendUrl + "reset-password?token=" + token;
        mailSender.send(member.getEmail(), "[Ounce] 비밀번호 재설정", buildBody(link));
    }

    private String buildBody(String link) {
        return """
            <div style="max-width:480px;margin:0 auto;padding:40px 24px;font-family:'Apple SD Gothic Neo',sans-serif;color:#1a1a1a;">
              <h1 style="margin:0 0 24px;font-size:22px;font-weight:700;">비밀번호 재설정</h1>
              <p style="margin:0 0 8px;font-size:15px;line-height:1.7;color:#555;">
                비밀번호 재설정을 요청하셨습니다.<br>
                아래 버튼을 눌러 새 비밀번호를 설정해주세요.
              </p>
              <a href="%s" style="display:inline-block;margin:28px 0;padding:14px 28px;background:#2d2d33;color:#fff;font-size:15px;font-weight:600;text-decoration:none;border-radius:8px;">
                비밀번호 재설정하기
              </a>
              <p style="margin:0 0 8px;font-size:13px;line-height:1.7;color:#888;">
                이 링크는 30분간 유효합니다.<br>
                본인이 요청하지 않으셨다면 이 메일을 무시하셔도 됩니다.
              </p>
              <p style="margin:24px 0 0;font-size:12px;color:#aaa;word-break:break-all;">
                버튼이 동작하지 않으면 아래 주소를 복사해 브라우저에 붙여넣어 주세요.<br>
                %s
              </p>
              <hr style="margin:32px 0 16px;border:0;border-top:1px solid #eee;">
              <p style="margin:0;font-size:12px;color:#aaa;">Ounce</p>
            </div>
            """.formatted(link, link);
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 링크입니다.");
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        member.changePassword(passwordEncoder.encode(newPassword));

        redisTemplate.delete(key);
        redisTemplate.delete("refresh:" + email);
    }

}
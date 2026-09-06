package ounce.market.demo.member.sender;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ounce.market.demo.member.entity.MailSender;

@Slf4j
@Component
@Profile("local")
public class LogMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("=== 메일 발송 (로컬) ===\nTo: {}\nSubject: {}\n{}", to, subject, body);
    }
}
package ounce.market.demo.member.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ounce.market.demo.member.entity.MailSender;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class SesMailSender implements MailSender {

    private final SesClient sesClient;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .source(from)
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(body).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build());
        } catch (SesException e) {
            log.error("메일 발송 실패: to={}", to, e);
        }
    }
}
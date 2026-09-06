package ounce.market.demo.member.entity;

public interface MailSender {
    void send(String to, String subject, String body);
}
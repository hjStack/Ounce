package ounce.market.demo.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.member.entity.Member;
import ounce.market.demo.member.repository.MemberRepository;
import ounce.market.demo.notification.entity.Notification;
import ounce.market.demo.notification.repository.NotificationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String MIDNIGHT_TYPE = "MIDNIGHT";

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void enableMidnightAlert(String email) {
        findMember(email).enableMidnightAlert();
    }

    @Transactional
    public void disableMidnightAlert(String email) {
        findMember(email).disableMidnightAlert();
    }

    @Transactional(readOnly = true)
    public boolean isMidnightAlertEnabled(String email) {
        return findMember(email).isMidnightAlertEnabled();
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String email) {
        return notificationRepository.findByMemberMemberIdOrderByCreatedAtDesc(
                findMember(email).getMemberId());
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!notification.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        notification.markAsRead();
    }

    @Transactional
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void createMidnightNotifications() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        memberRepository.findAllByMidnightAlertEnabledTrue().forEach(member -> {
            if (!notificationRepository.existsByMemberMemberIdAndTypeAndCreatedAtBetween(
                    member.getMemberId(), MIDNIGHT_TYPE, from, to)) {
                notificationRepository.save(Notification.builder()
                        .member(member)
                        .title("미드나이트 세일이 시작됐어요 🌙")
                        .content("오늘의 특가 상품을 지금 확인해보세요.")
                        .type(MIDNIGHT_TYPE)
                        .build());
            }
        });
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}

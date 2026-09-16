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
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import ounce.market.demo.notification.dto.request.PushSubscriptionRequest;
import ounce.market.demo.notification.dto.request.PushSubscriptionDeleteRequest;
import ounce.market.demo.notification.entity.PushSubscription;
import ounce.market.demo.notification.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String MIDNIGHT_TYPE = "MIDNIGHT";

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${app.web-push.public-key:}")
    private String webPushPublicKey;

    @Value("${app.web-push.private-key:}")
    private String webPushPrivateKey;

    @Value("${app.web-push.subject:mailto:no-reply@ouncefresh.com}")
    private String webPushSubject;

    @Transactional
    public void savePushSubscription(String email, PushSubscriptionRequest request) {
        Member member = findMember(email);
        pushSubscriptionRepository.findByEndpoint(request.endpoint())
                .ifPresentOrElse(subscription -> subscription.updateKeys(
                                request.p256dh(), request.auth(), member),
                        () -> pushSubscriptionRepository.save(new PushSubscription(
                                request.endpoint(), request.p256dh(), request.auth(), member)));
        member.enableMidnightAlert();
    }

    @Transactional
    public void deletePushSubscription(String email, PushSubscriptionDeleteRequest request) {
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.endpoint())
                .orElse(null);
        if (subscription != null && subscription.getMember().getEmail().equals(email)) {
            pushSubscriptionRepository.deleteByEndpoint(request.endpoint());
        }
    }

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
        sendMidnightPushes();
    }

    private void sendMidnightPushes() {
        if (webPushPublicKey.isBlank() || webPushPrivateKey.isBlank()) return;
        final PushService pushService;
        try {
            pushService = new PushService(webPushPublicKey, webPushPrivateKey, webPushSubject);
        } catch (Exception ignored) {
            return;
        }
        pushSubscriptionRepository.findAllByMemberMidnightAlertEnabledTrue().forEach(subscription -> {
            try {
                Subscription.Keys keys = new Subscription.Keys(subscription.getP256dh(), subscription.getAuth());
                pushService.send(new nl.martijndwars.webpush.Notification(
                        new Subscription(subscription.getEndpoint(), keys),
                        "{\"title\":\"미드나이트 세일이 시작됐어요 🌙\",\"body\":\"오늘의 특가 상품을 지금 확인해보세요.\",\"url\":\"/timedeal\"}"));
            } catch (Exception ignored) {
                // 만료된 브라우저 구독 하나가 전체 발송을 막지 않도록 한다.
            }
        });
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}

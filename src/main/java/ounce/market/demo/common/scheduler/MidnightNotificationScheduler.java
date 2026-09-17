package ounce.market.demo.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ounce.market.demo.common.Exception.ExpiredPushSubscriptionException;
import ounce.market.demo.common.service.WebPushService;
import ounce.market.demo.notification.entity.PushSubscription;
import ounce.market.demo.notification.repository.PushSubscriptionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class MidnightNotificationScheduler {

    private final PushSubscriptionRepository subscriptionRepository;
    private final WebPushService webPushService;

    @Scheduled(cron = "0 50 21 * * *", zone = "Asia/Seoul")
    public void sendMidnightSaleNotification() {
        List<PushSubscription> subscriptions =
                subscriptionRepository.findAllByMidnightAlertEnabledTrue();

        String payload = """
                  {
                    "title": "미드나이트 세일 알림",
                    "body": "오늘 밤 10시 미드나이트 세일이 시작됩니다.",
                    "url": "/timedeal"
                  }
                  """;

        for (PushSubscription subscription : subscriptions) {
            try {
                webPushService.send(subscription, payload);

            } catch (ExpiredPushSubscriptionException e) {
                // 푸시 구독만 삭제
                subscriptionRepository.delete(subscription);

                log.info(
                        "만료된 푸시 구독 삭제. pushSubscriptionId={}",
                        subscription.getId()
                );

            } catch (Exception e) {
                log.error(
                        "미드나이트 알림 발송 실패. pushSubscriptionId={}",
                        subscription.getId(),
                        e
                );
            }
        }
    }
}
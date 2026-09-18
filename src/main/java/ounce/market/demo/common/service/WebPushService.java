package ounce.market.demo.common.service;


import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.common.Exception.ExpiredPushSubscriptionException;
import ounce.market.demo.notification.entity.PushSubscription;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;

@Service
public class WebPushService {

    private final PushService pushService;

    public WebPushService(
            @Value("${web-push.public-key}") String publicKey,
            @Value("${web-push.private-key}") String privateKey,
            @Value("${web-push.subject}") String subject
    ) throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    public void send(PushSubscription subscription, String payload)
            throws Exception {

        Notification notification = new Notification(
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                payload.getBytes(StandardCharsets.UTF_8),
                3600 // TTL : 1H
        );

        HttpResponse response = pushService.send(notification);
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 404 || statusCode == 410) {
            throw new ExpiredPushSubscriptionException(
                    "만료된 푸시 구독입니다."
            );
        }
    }
}
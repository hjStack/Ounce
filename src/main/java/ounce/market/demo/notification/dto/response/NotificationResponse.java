package ounce.market.demo.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ounce.market.demo.notification.entity.Notification;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private String title;
    private String content;
    private String type;
    private boolean is_read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(), notification.getTitle(),
                notification.getContent(), notification.getType(),
                notification.is_read(), notification.getCreatedAt()
        );
    }
}

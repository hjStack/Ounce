package ounce.market.demo.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.common.BaseEntity;
import ounce.market.demo.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue
    private Long notificationId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean is_read;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public Notification(Member member, String title, String content, String type) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.type = type;
        this.is_read = false;
    }

    public void markAsRead() {
        this.is_read = true;
    }
}

package ounce.market.demo.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pushSubscriptionId;

    @Column(nullable = false, unique = true, length = 1000)
    private String endpoint;

    @Column(nullable = false, length = 500)
    private String p256dh;

    @Column(nullable = false, length = 500)
    private String auth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private boolean midnightAlertEnabled = true;

    public PushSubscription(String endpoint, String p256dh, String auth, Member member) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.member = member;
    }

    public void updateKeys(String p256dh, String auth, Member member) {
        this.p256dh = p256dh;
        this.auth = auth;
        this.member = member;
    }

    public long getId() {
        return pushSubscriptionId;
    }

    public boolean isMidnightAlertEnabled() {
        return midnightAlertEnabled;
    }

    public void disableMidnightAlert() {
        this.midnightAlertEnabled = false;
    }
}

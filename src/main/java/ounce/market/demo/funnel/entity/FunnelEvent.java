package ounce.market.demo.funnel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_session_created_at", columnList = "session_id, created_at"),
        @Index(name = "idx_events_type_created_at", columnList = "event_type, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FunnelEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public FunnelEvent(String sessionId, Long userId, String eventType,
                       String metadata, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.eventType = eventType;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
}

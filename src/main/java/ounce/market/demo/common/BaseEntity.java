package ounce.market.demo.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// created_at, updated_at 공통 클래스
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
// JPA의 엔티티의 변경 이벤트를 감지하여 특정 동작을 수행하는 리스너를 연결하는데 사용

// 엔티티의 생성/수정 시간 또는 생성자/수정자를 자동으로 기록하고 싶을 때 사용됩니다.
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
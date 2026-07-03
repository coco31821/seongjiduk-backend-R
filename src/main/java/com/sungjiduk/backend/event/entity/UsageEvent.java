package com.sungjiduk.backend.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 접속/이용 이벤트 1건. 관리자 통계(접속자/이용)의 원천 데이터.
 * user_id는 비회원이면 null이며, 비회원 식별은 session_id(익명 UUID)로 한다.
 */
@Entity
@Table(name = "usage_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 인증 사용자면 채우고, 비회원이면 null */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    /** 화면 경로 등 (예: /map) */
    @Column(length = 255)
    private String path;

    /** 이벤트 대상 식별자 (예: CONTENT_SELECTED의 contentId) */
    @Column(name = "target_id")
    private Long targetId;

    /** 비회원 세션 식별자 */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    private UsageEvent(Long userId, EventType eventType, String path, Long targetId, String sessionId) {
        this.userId = userId;
        this.eventType = eventType;
        this.path = path;
        this.targetId = targetId;
        this.sessionId = sessionId;
        this.occurredAt = LocalDateTime.now();
    }
}

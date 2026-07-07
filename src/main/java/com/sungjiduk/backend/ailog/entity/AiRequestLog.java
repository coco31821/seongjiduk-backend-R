package com.sungjiduk.backend.ailog.entity;

import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 호출 로그 (04 ERD AiRequestLog).
 * 생성/재생성 API(B파트)가 트랜잭션과 함께 기록하고, 관리자 통계(C파트)는 읽기 전용으로 집계한다.
 */
@Entity
@Table(name = "ai_request_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 비회원 생성도 가능하므로 null 허용 (Trip↔User 배선 후 인증 사용자로 채움) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_plan_id")
    private TripPlan tripPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private AiRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiRequestStatus status;

    /** ai-service가 토큰 사용량을 아직 반환하지 않아 null 허용 (후속 확장 슬롯) */
    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiRequestLog(User user, TripPlan tripPlan, AiRequestType requestType,
                         AiRequestStatus status, Integer tokenUsage) {
        this.user = user;
        this.tripPlan = tripPlan;
        this.requestType = requestType;
        this.status = status;
        this.tokenUsage = tokenUsage;
        this.createdAt = LocalDateTime.now();
    }
}

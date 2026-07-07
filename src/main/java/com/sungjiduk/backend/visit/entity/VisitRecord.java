package com.sungjiduk.backend.visit.entity;

import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 방문 인증/메모 (04 ERD VisitRecord).
 * 사용자의 실측 방문 데이터 — 검증 신뢰도 레이어의 3단(자체 데이터) 축.
 */
@Entity
@Table(name = "visit_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private PilgrimageSpot spot;

    /** 어느 일정에서의 방문인지 — 일정 없이 인증만 남길 수도 있어 null 허용 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_plan_id")
    private TripPlan tripPlan;

    @Column(length = 1000)
    private String note;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    @Builder
    private VisitRecord(User user, PilgrimageSpot spot, TripPlan tripPlan,
                        String note, String imageUrl, LocalDateTime visitedAt) {
        this.user = user;
        this.spot = spot;
        this.tripPlan = tripPlan;
        this.note = note;
        this.imageUrl = imageUrl;
        this.visitedAt = visitedAt == null ? LocalDateTime.now() : visitedAt;
    }

    /** 소유자 확인 — 삭제 등 소유자 전용 행위의 가드 */
    public boolean isOwnedBy(Long userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }
}

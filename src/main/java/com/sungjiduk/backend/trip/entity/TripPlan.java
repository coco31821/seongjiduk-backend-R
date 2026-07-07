package com.sungjiduk.backend.trip.entity;

import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 여행 일정(루트)의 최상위 엔티티.
 * content는 필수로 연결하고, user는 비회원 일정 생성을 위해 null을 허용한다.
 */
@Entity
@Table(name = "trip_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 비회원 생성 일정은 null 가능 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_location", length = 200)
    private String startLocation;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    /** DTO와 동일하게 문자열 태그(LOW/NORMAL/HIGH)로 보관 */
    @Column(name = "budget_level", length = 20)
    private String budgetLevel;

    @Column(name = "travel_style", length = 30)
    private String travelStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    /** 공유/비로그인 상세 조회용 토큰 */
    @Column(name = "share_token", length = 64, unique = true)
    private String shareToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tripPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDay> days = new ArrayList<>();

    @Builder
    private TripPlan(User user, Content content, String title, String startLocation,
                     int durationDays, String budgetLevel, String travelStyle,
                     TripStatus status, String shareToken) {
        this.user = user;
        this.content = content;
        this.title = title;
        this.startLocation = startLocation;
        this.durationDays = durationDays;
        this.budgetLevel = budgetLevel;
        this.travelStyle = travelStyle;
        this.status = status == null ? TripStatus.DRAFT : status;
        this.shareToken = shareToken;
        this.createdAt = LocalDateTime.now();
    }

    /** 양방향 연관관계 편의 메서드 */
    public void addDay(TripDay day) {
        this.days.add(day);
        day.assignPlan(this);
    }

    public void markSaved() {
        this.status = TripStatus.SAVED;
    }

    /** AI가 생성한 제목으로 교체(빈 값이면 유지). */
    public void changeTitle(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
    }

    /** 공유 토큰을 최초 1회만 발급한다(멱등). 이미 있으면 유지해 공유 링크가 고정되게 한다. */
    public void assignShareToken(String token) {
        if (this.shareToken == null) {
            this.shareToken = token;
        }
    }

    /** 비회원이 만든 플랜(소유자 없음) 여부 */
    public boolean isUnowned() {
        return user == null;
    }

    public boolean isOwnedBy(Long userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }

    /** 비회원 생성 플랜을 로그인 사용자가 저장/공유할 때 소유권을 가져간다 */
    public void assignOwner(User owner) {
        this.user = owner;
    }
}

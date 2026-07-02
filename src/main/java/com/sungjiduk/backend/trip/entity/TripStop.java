package com.sungjiduk.backend.trip.entity;

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

/**
 * 하루 일정 내 개별 방문 장소.
 * spotType 에 따라 pilgrimageSpotId 또는 nearbyAttractionId 중 하나만 채운다.
 * (DB CHECK 제약 + 애플리케이션 검증으로 보장 — 04_도메인_모델_ERD 설계메모 참고)
 */
@Entity
@Table(name = "trip_stop")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 20)
    private SpotType spotType;

    @Column(name = "pilgrimage_spot_id")
    private Long pilgrimageSpotId;

    @Column(name = "nearby_attraction_id")
    private Long nearbyAttractionId;

    @Column(name = "seq_no", nullable = false)
    private int sequence;

    /** MVP: mock 시간 문자열(예: "10:00") */
    @Column(name = "arrival_time", length = 10)
    private String arrivalTime;

    @Column(name = "stay_minutes")
    private int stayMinutes;

    @Builder
    private TripStop(SpotType spotType, Long pilgrimageSpotId, Long nearbyAttractionId,
                    int sequence, String arrivalTime, int stayMinutes) {
        this.spotType = spotType;
        this.pilgrimageSpotId = pilgrimageSpotId;
        this.nearbyAttractionId = nearbyAttractionId;
        this.sequence = sequence;
        this.arrivalTime = arrivalTime;
        this.stayMinutes = stayMinutes;
    }

    void assignDay(TripDay tripDay) {
        this.tripDay = tripDay;
    }
}

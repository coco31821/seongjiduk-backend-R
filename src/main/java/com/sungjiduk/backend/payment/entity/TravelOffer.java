package com.sungjiduk.backend.payment.entity;

import com.sungjiduk.backend.common.BaseEntity;
import com.sungjiduk.backend.payment.constants.TravelOfferProvider;
import com.sungjiduk.backend.payment.constants.TravelOfferType;
import com.sungjiduk.backend.trip.entity.TripPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 확정된 일정에 붙여 보여줄 항공권/숙소 후보 스냅샷.
 * Mock 데이터로 시작하되, externalOfferId/rawPayload로 실제 공급자 API 응답까지 보존한다.
 */
@Entity
@Table(name = "travel_offer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelOffer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_plan_id", nullable = false)
    private TripPlan tripPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TravelOfferProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false, length = 20)
    private TravelOfferType offerType;

    @Column(name = "external_offer_id", length = 120)
    private String externalOfferId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    // Toss 응답 원문(수 KB) 저장 — @Lob은 MySQL에서 TINYTEXT(255)로 매핑돼 넘침. TEXT 명시(마이그레이션 V9와 일치).
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Builder
    private TravelOffer(TripPlan tripPlan, TravelOfferProvider provider, TravelOfferType offerType,
                        String externalOfferId, String name, String description, Long price,
                        String currency, LocalDateTime validUntil, String rawPayload) {
        this.tripPlan = tripPlan;
        this.provider = provider;
        this.offerType = offerType;
        this.externalOfferId = externalOfferId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency == null ? "KRW" : currency;
        this.validUntil = validUntil;
        this.rawPayload = rawPayload;
    }

    public boolean isExpired(LocalDateTime now) {
        return validUntil != null && validUntil.isBefore(now);
    }
}

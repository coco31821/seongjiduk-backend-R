package com.sungjiduk.backend.payment.entity;

import com.sungjiduk.backend.common.BaseEntity;
import com.sungjiduk.backend.payment.constants.SupplierBookingStatus;
import com.sungjiduk.backend.payment.constants.TravelOfferProvider;
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
 * 결제 후 성지덕이 외부 공급자에게 예약/구매 요청한 결과.
 * Mock 플로우에서는 외부 API 대신 CONFIRMED 상태와 mock 예약번호를 저장한다.
 */
@Entity
@Table(name = "supplier_booking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierBooking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TravelOfferProvider provider;

    @Column(name = "external_reservation_id", length = 120)
    private String externalReservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupplierBookingStatus status;

    @Column(name = "paid_amount", nullable = false)
    private Long paidAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "fail_reason", length = 1000)
    private String failReason;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @Builder
    private SupplierBooking(TravelOfferProvider provider, String externalReservationId,
                            SupplierBookingStatus status, Long paidAmount, String currency,
                            LocalDateTime requestedAt, LocalDateTime confirmedAt,
                            String failReason, String rawPayload) {
        this.provider = provider;
        this.externalReservationId = externalReservationId;
        this.status = status == null ? SupplierBookingStatus.REQUESTED : status;
        this.paidAmount = paidAmount;
        this.currency = currency == null ? "KRW" : currency;
        this.requestedAt = requestedAt == null ? LocalDateTime.now() : requestedAt;
        this.confirmedAt = confirmedAt;
        this.failReason = failReason;
        this.rawPayload = rawPayload;
    }

    public static SupplierBooking mockConfirmed(TravelOfferProvider provider, String reservationId,
                                                Long paidAmount, String currency, String rawPayload) {
        LocalDateTime now = LocalDateTime.now();
        return SupplierBooking.builder()
                .provider(provider)
                .externalReservationId(reservationId)
                .status(SupplierBookingStatus.CONFIRMED)
                .paidAmount(paidAmount)
                .currency(currency)
                .requestedAt(now)
                .confirmedAt(now)
                .rawPayload(rawPayload)
                .build();
    }

    public void markConfirmed(String reservationId, String rawPayload) {
        this.externalReservationId = reservationId;
        this.status = SupplierBookingStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.rawPayload = rawPayload;
    }

    public void markFailed(String failReason, String rawPayload) {
        this.status = SupplierBookingStatus.FAILED;
        this.failReason = failReason;
        this.rawPayload = rawPayload;
    }

    void assignOrder(PaymentOrder paymentOrder) {
        this.paymentOrder = paymentOrder;
    }
}

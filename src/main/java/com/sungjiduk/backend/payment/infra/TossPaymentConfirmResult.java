package com.sungjiduk.backend.payment.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentConfirmResult(
        String paymentKey,
        String orderId,
        String status,
        String method,
        Long totalAmount,
        String currency,
        OffsetDateTime approvedAt,
        Receipt receipt,
        String rawPayload
) {
    public String receiptUrl() {
        return receipt == null ? null : receipt.url();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(String url) {
    }
}

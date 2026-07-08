package com.sungjiduk.backend.payment.dto.response;

public record PaymentOrderCreateResponse(
        String orderId,
        String orderName,
        Long amount,
        String currency,
        String clientKey
) {
}

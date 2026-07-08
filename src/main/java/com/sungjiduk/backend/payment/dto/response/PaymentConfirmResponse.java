package com.sungjiduk.backend.payment.dto.response;

import com.sungjiduk.backend.payment.constants.PaymentOrderStatus;

import java.util.List;

public record PaymentConfirmResponse(
        String orderId,
        PaymentOrderStatus status,
        String paymentKey,
        String method,
        Long amount,
        String currency,
        String receiptUrl,
        List<SupplierBookingResponse> supplierBookings
) {
}

package com.sungjiduk.backend.payment.dto.response;

import com.sungjiduk.backend.payment.entity.SupplierBooking;
import com.sungjiduk.backend.payment.constants.SupplierBookingStatus;
import com.sungjiduk.backend.payment.constants.TravelOfferProvider;

public record SupplierBookingResponse(
        TravelOfferProvider provider,
        String externalReservationId,
        SupplierBookingStatus status,
        Long paidAmount,
        String currency
) {
    public static SupplierBookingResponse from(SupplierBooking booking) {
        return new SupplierBookingResponse(
                booking.getProvider(),
                booking.getExternalReservationId(),
                booking.getStatus(),
                booking.getPaidAmount(),
                booking.getCurrency()
        );
    }
}

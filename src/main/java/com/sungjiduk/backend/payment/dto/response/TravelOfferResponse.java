package com.sungjiduk.backend.payment.dto.response;

import com.sungjiduk.backend.payment.entity.TravelOffer;
import com.sungjiduk.backend.payment.constants.TravelOfferProvider;
import com.sungjiduk.backend.payment.constants.TravelOfferType;

import java.time.LocalDateTime;

public record TravelOfferResponse(
        Long offerId,
        TravelOfferProvider provider,
        TravelOfferType offerType,
        String name,
        String description,
        Long price,
        String currency,
        LocalDateTime validUntil
) {
    public static TravelOfferResponse from(TravelOffer offer) {
        return new TravelOfferResponse(
                offer.getId(),
                offer.getProvider(),
                offer.getOfferType(),
                offer.getName(),
                offer.getDescription(),
                offer.getPrice(),
                offer.getCurrency(),
                offer.getValidUntil()
        );
    }
}

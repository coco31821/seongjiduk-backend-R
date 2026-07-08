package com.sungjiduk.backend.payment.dto.response;

import java.util.List;

public record TravelOffersResponse(
        List<TravelOfferResponse> flights,
        List<TravelOfferResponse> hotels
) {
}

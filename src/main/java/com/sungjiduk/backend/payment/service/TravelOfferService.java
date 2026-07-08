package com.sungjiduk.backend.payment.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.payment.dto.response.TravelOfferResponse;
import com.sungjiduk.backend.payment.dto.response.TravelOffersResponse;
import com.sungjiduk.backend.payment.entity.TravelOffer;
import com.sungjiduk.backend.payment.constants.TravelOfferProvider;
import com.sungjiduk.backend.payment.constants.TravelOfferType;
import com.sungjiduk.backend.payment.repository.TravelOfferRepository;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TravelOfferService {

    private final TripPlanRepository tripPlanRepository;
    private final TravelOfferRepository travelOfferRepository;

    public TravelOfferService(TripPlanRepository tripPlanRepository, TravelOfferRepository travelOfferRepository) {
        this.tripPlanRepository = tripPlanRepository;
        this.travelOfferRepository = travelOfferRepository;
    }

    @Transactional
    public TravelOffersResponse findOrCreateMockOffers(Long userId, Long tripPlanId) {
        TripPlan tripPlan = tripPlanRepository.findById(tripPlanId)
                .orElseThrow(() -> new TripNotFoundException(tripPlanId));
        requirePaymentReadyTrip(tripPlan, userId);

        List<TravelOffer> offers = travelOfferRepository.findByTripPlanIdAndDeletedAtIsNullOrderByIdAsc(tripPlanId);
        if (offers.isEmpty()) {
            offers = travelOfferRepository.saveAll(mockOffersFor(tripPlan));
        }
        return toResponse(offers);
    }

    private List<TravelOffer> mockOffersFor(TripPlan tripPlan) {
        int days = Math.max(tripPlan.getDurationDays(), 1);
        LocalDateTime validUntil = LocalDateTime.now().plusMinutes(30);
        long flightPrice = 180_000L + (days * 25_000L);
        long hotelPrice = days * 90_000L;
        String title = tripPlan.getTitle() == null ? "성지순례 일정" : tripPlan.getTitle();

        return List.of(
                TravelOffer.builder()
                        .tripPlan(tripPlan)
                        .provider(TravelOfferProvider.MOCK_FLIGHT)
                        .offerType(TravelOfferType.FLIGHT)
                        .externalOfferId("MOCK-FLIGHT-" + tripPlan.getId())
                        .name(title + " 왕복 항공권")
                        .description("성지덕 mock 왕복 항공권입니다.")
                        .price(flightPrice)
                        .currency("KRW")
                        .validUntil(validUntil)
                        .rawPayload("{\"provider\":\"MOCK_FLIGHT\"}")
                        .build(),
                TravelOffer.builder()
                        .tripPlan(tripPlan)
                        .provider(TravelOfferProvider.MOCK_HOTEL)
                        .offerType(TravelOfferType.HOTEL)
                        .externalOfferId("MOCK-HOTEL-" + tripPlan.getId())
                        .name(title + " 숙소")
                        .description(days + "일 일정 기준 성지덕 mock 숙소입니다.")
                        .price(hotelPrice)
                        .currency("KRW")
                        .validUntil(validUntil)
                        .rawPayload("{\"provider\":\"MOCK_HOTEL\"}")
                        .build()
        );
    }

    private void requirePaymentReadyTrip(TripPlan tripPlan, Long userId) {
        if (tripPlan.isUnowned() || !tripPlan.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (tripPlan.getStatus() != TripStatus.SAVED) {
            throw new BusinessException(ErrorCode.TRIP_NOT_READY_FOR_PAYMENT);
        }
    }

    private TravelOffersResponse toResponse(List<TravelOffer> offers) {
        List<TravelOfferResponse> flights = offers.stream()
                .filter(offer -> offer.getOfferType() == TravelOfferType.FLIGHT)
                .map(TravelOfferResponse::from)
                .toList();
        List<TravelOfferResponse> hotels = offers.stream()
                .filter(offer -> offer.getOfferType() == TravelOfferType.HOTEL)
                .map(TravelOfferResponse::from)
                .toList();
        return new TravelOffersResponse(flights, hotels);
    }
}

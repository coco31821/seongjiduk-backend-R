package com.sungjiduk.backend.payment.repository;

import com.sungjiduk.backend.payment.entity.TravelOffer;
import com.sungjiduk.backend.payment.constants.TravelOfferType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelOfferRepository extends JpaRepository<TravelOffer, Long> {

    List<TravelOffer> findByTripPlanIdAndDeletedAtIsNullOrderByIdAsc(Long tripPlanId);

    List<TravelOffer> findByTripPlanIdAndOfferTypeAndDeletedAtIsNullOrderByIdAsc(
            Long tripPlanId,
            TravelOfferType offerType
    );
}

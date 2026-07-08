package com.sungjiduk.backend.payment.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.payment.dto.response.TravelOffersResponse;
import com.sungjiduk.backend.payment.service.TravelOfferService;
import com.sungjiduk.backend.user.entity.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/offers")
public class TravelOfferController {

    private final TravelOfferService travelOfferService;

    public TravelOfferController(TravelOfferService travelOfferService) {
        this.travelOfferService = travelOfferService;
    }

    @GetMapping
    public ApiResponse<TravelOffersResponse> offers(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok(travelOfferService.findOrCreateMockOffers(requireUserId(currentUser), tripId));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.getId();
    }
}

package com.sungjiduk.backend.trip.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.user.entity.CurrentUser;
import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.service.TripService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/generate")
    public ApiResponse<TripResponse> generate(@AuthenticationPrincipal CurrentUser currentUser,
                                              @Valid @RequestBody TripGenerateRequest request) {
        return ApiResponse.ok(tripService.generate(idOf(currentUser), request));
    }

    @PostMapping("/{tripId}/regenerate")
    public ApiResponse<TripResponse> regenerate(@AuthenticationPrincipal CurrentUser currentUser,
                                                @PathVariable Long tripId, @Valid @RequestBody TripGenerateRequest request) {
        return ApiResponse.ok(tripService.regenerate(idOf(currentUser), tripId, request));
    }

    @PostMapping("/{tripId}/save")
    public ApiResponse<TripSummaryResponse> save(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.save(currentUser.getId(), tripId));
    }

    @GetMapping
    public ApiResponse<List<TripSummaryResponse>> trips(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(tripService.findMyTrips(currentUser.getId()));
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripResponse> trip(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.findTrip(tripId));
    }

    @DeleteMapping("/{tripId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser currentUser,
                                    @PathVariable Long tripId) {
        tripService.delete(currentUser.getId(), tripId);
        return ApiResponse.ok();
    }

    @PostMapping("/{tripId}/share")
    public ApiResponse<TripShareResponse> share(@AuthenticationPrincipal CurrentUser currentUser,
                                                @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.share(currentUser.getId(), tripId));
    }

    /** 선택 인증 엔드포인트용 — 비회원이면 null */
    private Long idOf(CurrentUser currentUser) {
        return currentUser == null ? null : currentUser.getId();
    }
}

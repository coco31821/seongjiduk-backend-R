package com.sungjiduk.backend.trip.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.service.TripService;
import jakarta.validation.Valid;
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
    public ApiResponse<TripResponse> generate(@Valid @RequestBody TripGenerateRequest request) {
        return ApiResponse.ok(tripService.generate(request));
    }

    @PostMapping("/{tripId}/regenerate")
    public ApiResponse<TripResponse> regenerate(@PathVariable Long tripId, @Valid @RequestBody TripGenerateRequest request) {
        return ApiResponse.ok(tripService.regenerate(tripId, request));
    }

    @PostMapping("/{tripId}/save")
    public ApiResponse<TripSummaryResponse> save(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.save(tripId));
    }

    @GetMapping
    public ApiResponse<List<TripSummaryResponse>> trips() {
        return ApiResponse.ok(tripService.findMyTrips());
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripResponse> trip(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.findTrip(tripId));
    }

    @DeleteMapping("/{tripId}")
    public ApiResponse<Void> delete(@PathVariable Long tripId) {
        tripService.delete(tripId);
        return ApiResponse.ok();
    }

    @PostMapping("/{tripId}/share")
    public ApiResponse<TripShareResponse> share(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.share(tripId));
    }
}

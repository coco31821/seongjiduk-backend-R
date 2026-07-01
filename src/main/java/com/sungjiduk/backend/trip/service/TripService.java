package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripService {

    public TripResponse generate(TripGenerateRequest request) {
        return mockTrip(10L, request.durationDays());
    }

    public TripResponse regenerate(Long tripId, TripGenerateRequest request) {
        return mockTrip(tripId, request.durationDays());
    }

    public TripSummaryResponse save(Long tripId) {
        return new TripSummaryResponse(tripId, "러브라이브! 뮤즈 성지순례", 3, "SAVED");
    }

    public List<TripSummaryResponse> findMyTrips() {
        return List.of(new TripSummaryResponse(10L, "러브라이브! 뮤즈 성지순례", 3, "SAVED"));
    }

    public TripResponse findTrip(Long tripId) {
        return mockTrip(tripId, 3);
    }

    public void delete(Long tripId) {
    }

    public TripShareResponse share(Long tripId) {
        return new TripShareResponse(tripId, "https://seongjiduk.example/trips/" + tripId, "러브라이브! 뮤즈 성지순례 2박 3일 루트");
    }

    private TripResponse mockTrip(Long tripId, int durationDays) {
        return new TripResponse(
                tripId,
                "러브라이브! 뮤즈 " + durationDays + "일 성지순례",
                List.of(new TripResponse.DayPlan(
                        1,
                        "아키하바라 주변 성지 중심 일정",
                        List.of(new TripResponse.Stop(
                                1,
                                "PILGRIMAGE",
                                1L,
                                "쇼헤이바시",
                                "10:00",
                                30,
                                "작품 주요 장면과 연결된 대표 성지입니다."
                        ))
                )),
                "러브라이브! 뮤즈 성지순례 루트"
        );
    }
}

package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.trip.repository.TripStopRepository;
import com.sungjiduk.backend.user.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final UsageEventRepository usageEventRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TripStopRepository tripStopRepository;
    private final PilgrimageSpotRepository pilgrimageSpotRepository;

    public AdminStatsOverviewResponse overview() throws NoSuchElementException {
        long userTotalCount = userRepository.count();
        long todayVisitorCount = usageEventRepository.countUsageEventByOccurredAtBetween(start(), end());
        long todayTripPlanCount = tripPlanRepository.count();
        long AiRequestCount = 9999L; // Phase 2에서 구현 예정, AiRequestRepository

        List<String> ContentTodayList = tripPlanRepository.findMostFrequentTitleToday(start(), end(), PageRequest.of(0, 1));
        String topContentToday;

        if (ContentTodayList.isEmpty()) {
            topContentToday = "아직 집계된 작품이 없습니다.";
        }

        else {
            topContentToday = ContentTodayList.getFirst();
        }

        List<Long> SpotTodayList = tripStopRepository.findMostFrequentSpotToday(start(), end(), PageRequest.of(0, 1));
        String topSpotToday;

        if (SpotTodayList.isEmpty()) {
            topSpotToday = "아직 집계된 성지가 없습니다";
        }

        else {
           Long id = SpotTodayList.getFirst();
            Optional<PilgrimageSpot> optionalPilgrimageSpot = pilgrimageSpotRepository.findById(id);


            if (optionalPilgrimageSpot.isEmpty()) {
                throw new NoSuchElementException();
            }

            topSpotToday = optionalPilgrimageSpot.get().getName();
        }

        return new AdminStatsOverviewResponse(
            userTotalCount,
            todayVisitorCount,
            todayTripPlanCount,
            AiRequestCount,
            topContentToday,
            topSpotToday);
    }

    public StatsSeriesResponse visitors() {
        return new StatsSeriesResponse("visitors", List.of(
                new StatsSeriesResponse.Point("2026-07-01", 34),
                new StatsSeriesResponse.Point("2026-07-02", 41)
        ));
    }

    public StatsSeriesResponse usage() {
        return new StatsSeriesResponse("trip_plans", List.of(
                new StatsSeriesResponse.Point("trip_generate", 88),
                new StatsSeriesResponse.Point("ai_request", 103)
        ));
    }

    private LocalDateTime start() {
        return LocalDateTime.now().toLocalDate().atStartOfDay(); // 오늘 0시 0분 0초
    }

    private LocalDateTime end() {
        return LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX); // 오늘 23시 59분 59.99999...초
    }
}

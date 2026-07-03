package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.user.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ContentRepository contentRepository;

    public AdminStatsOverviewResponse overview() {
        Content content;
        try {
            Long frequentContentId = tripPlanRepository.findFrequentContent(Pageable.ofSize(1)).getFirst();
            content = contentRepository.findById(frequentContentId).orElseThrow();
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e);
        }

        return new AdminStatsOverviewResponse(
            userRepository.count(),
            usageEventRepository.countUsageEventByCreatedAtBetween(
                (LocalDateTime.now().toLocalDate().atStartOfDay()),
                LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX)),
                tripPlanRepository.count(),
            103L, // AiRequestLogRepository.count(),
            content.getTitle(),
            "쇼헤이바시");
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
}

package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.trip.repository.TripStopRepository;
import com.sungjiduk.backend.user.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        LocalDate today = LocalDate.now();

        long userTotalCount = userRepository.count();
        long todayVisitorCount = usageEventRepository.countUsageEventByOccurredAtBetween(start(today), end(today));
        long todayTripPlanCount = tripPlanRepository.count();
        long AiRequestCount = 9999L; // Phase 2에서 구현 예정, AiRequestRepository

        String topContentToday = getContentTitle(today, today);
        String topSpotToday = getSpotName(today, today);

        return new AdminStatsOverviewResponse(
            userTotalCount,
            todayVisitorCount,
            todayTripPlanCount,
            AiRequestCount,
            topContentToday,
            topSpotToday);
    }

    public StatsSeriesResponse visitors() {
        LocalDate today = LocalDate.now();

        long todayVisitorCount = usageEventRepository.countUsageEventByOccurredAtBetween(start(today), end(today));
        long todaySignUpCount = userRepository.countUserByCreatedAtBetween(start(today), end(today));

        return new StatsSeriesResponse("접속자 통계", List.of(
                new StatsSeriesResponse.Point("오늘 방문 수", todayVisitorCount),
                new StatsSeriesResponse.Point("DAU", 41),
                new StatsSeriesResponse.Point("회원가입 수", todaySignUpCount),
                new StatsSeriesResponse.Point("비회원/회원 이용 비율", 41)
        ));
    }

    public StatsSeriesResponse usage() {
        LocalDate today = LocalDate.now();

        long totalTripPlanCount = tripPlanRepository.count();
        long todayTripPlanCount = tripPlanRepository.countTripPlanByCreatedAtBetween(start(today), end(today));
        String topContentToday = getContentTitle(today, today);
        String topSpotToday = getSpotName(today, today);


        return new StatsSeriesResponse("서비스 이용 통계", List.of(
                new StatsSeriesResponse.Point("총 일정 생성 수", totalTripPlanCount),
                new StatsSeriesResponse.Point("일자별 일정 생성 수", todayTripPlanCount),
                new StatsSeriesResponse.Point("AI 호출 수", 103), // P2때 작업 예정
                new StatsSeriesResponse.Point("인기 작품", 103),
                new StatsSeriesResponse.Point("인기 국가/도시", 103),
                new StatsSeriesResponse.Point("인기 성지", 103),
                new StatsSeriesResponse.Point("예산 태그 분포", 103),
                new StatsSeriesResponse.Point("저장 수", 103),
                new StatsSeriesResponse.Point("공유 수", 103)
        ));
    }

    private String getContentTitle(LocalDate startDate, LocalDate endDate) {
        List<Content> contentList = tripPlanRepository.findMostFrequentContent(
            start(startDate),
            end(endDate),
            PageRequest.of(0, 1)
        );

        try {
            return contentList.getFirst().getTitle();
        } catch (Exception e) {
            return "아직 집계된 작품이 없습니다.";
        }
    }

    private String getSpotName(LocalDate startDate, LocalDate endDate) {
        List<Long> spotList = tripStopRepository.findMostFrequentSpotToday(
            start(startDate),
            end(endDate),
            PageRequest.of(0, 1)
        );

        try {
            Optional<PilgrimageSpot> pilgrimageSpot = pilgrimageSpotRepository.findById(spotList.getFirst());

            if (pilgrimageSpot.isEmpty()) {
                throw new BusinessException(ErrorCode.PILGRIMAGE_SPOT_NOT_FOUND);
            }

            return pilgrimageSpot.get().getName();

        } catch (NoSuchElementException e) {
            return "아직 집계된 성지가 없습니다.";
        }
    }

    private LocalDateTime start(LocalDate time) {
        return time.atStartOfDay(); // 0시 0분 0초
    }

    private LocalDateTime end(LocalDate time) {
        return time.atTime(LocalTime.MAX); // 23시 59분 59.99999...초
    }
}

package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.content.entity.Content;
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
    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository pilgrimageSpotRepository;

    public AdminStatsOverviewResponse overview() throws NoSuchElementException {
        long userTotalCount = userRepository.count();
        long todayVisitorCount = usageEventRepository.countUsageEventByOccurredAtBetween(start(), end());
        long todayTripPlanCount = tripPlanRepository.count();
        long AiRequestCount = 9999L; // Phase 2에서 구현 예정, AiRequestRepository

        String topContentToday = getContentTitle(
            tripPlanRepository.findMostFrequentContentIdToday(
                start(),
                end(),
                PageRequest.of(0, 1)
            )
        );

        List<Long> spotTodayList = tripStopRepository.findMostFrequentSpotToday(
            start(),
            end(),
            PageRequest.of(0, 1)
        );

        String topSpotToday = getSpotName(spotTodayList);

        return new AdminStatsOverviewResponse(
            userTotalCount,
            todayVisitorCount,
            todayTripPlanCount,
            AiRequestCount,
            topContentToday,
            topSpotToday);
    }

    private String getContentTitle(List<Long> contentTodayList) {
        String topContentToday;

        if (contentTodayList.isEmpty()) {
            topContentToday = "아직 집계된 작품이 없습니다.";
        }

        else {
            Optional<Content> optionalContent = contentRepository.findById(contentTodayList.getFirst());

            Content content = optionalContent.orElseThrow(NoSuchElementException::new);

            topContentToday = content.getTitle();
        }

        return topContentToday;
    }

    private String getSpotName(List<Long> spotTodayList) {
        String spot;

        if (spotTodayList.isEmpty()) {
            spot = "아직 집계된 성지가 없습니다";
        }

        else {
            Long id = spotTodayList.getFirst();
            Optional<PilgrimageSpot> optionalPilgrimageSpot = pilgrimageSpotRepository.findById(id);


            if (optionalPilgrimageSpot.isEmpty()) {
                throw new NoSuchElementException();
            }

            spot = optionalPilgrimageSpot.get().getName();
        }

        return spot;
    }

    public StatsSeriesResponse visitors() {
        long todayVisitorCount = usageEventRepository.countUsageEventByOccurredAtBetween(start(), end());
        long todaySignUpCount = userRepository.countUserByCreatedAtBetween(start(), end());

        return new StatsSeriesResponse("접속자 통계", List.of(
                new StatsSeriesResponse.Point("오늘 방문 수", todayVisitorCount),
                new StatsSeriesResponse.Point("DAU", 41),
                new StatsSeriesResponse.Point("회원가입 수", todaySignUpCount),
                new StatsSeriesResponse.Point("비회원/회원 이용 비율", 41)
        ));
    }

    public StatsSeriesResponse usage() {
        long totalTripPlanCount = tripPlanRepository.count();
        long todayTripPlanCount = tripPlanRepository.countTripPlanByCreatedAtBetween(start(), end());
        String topContentToday = getContentTitle(tripPlanRepository.findMostFrequentContentIdToday(start(), end(), PageRequest.of(0, 1)));
        List<Long> spotTodayList = tripStopRepository.findMostFrequentSpotToday(start(), end(), PageRequest.of(0, 1));
        String topSpotToday = getSpotName(spotTodayList);


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

    private LocalDateTime start() {
        return LocalDateTime.now().toLocalDate().atStartOfDay(); // 오늘 0시 0분 0초
    }

    private LocalDateTime end() {
        return LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX); // 오늘 23시 59분 59.99999...초
    }
}

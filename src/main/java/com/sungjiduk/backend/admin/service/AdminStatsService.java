package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.event.entity.EventType;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.trip.repository.TripStopRepository;
import com.sungjiduk.backend.user.repository.UserRepository;

import org.jspecify.annotations.NonNull;
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

        String topContentToday = getContentTitle(today, today);
        String topSpotToday = getSpotName(today, today);

        return new AdminStatsOverviewResponse(
            userTotalCount,
            todayVisitorCount,
            todayTripPlanCount,
            topContentToday,
            topSpotToday);
    }

    public StatsSeriesResponse visitors() {
        LocalDate today = LocalDate.now();

        String todayVisitorCount = usageEventRepository.countUsageEventByOccurredAtBetween(start(today), end(today)).toString();
        String todaySignUpCount = userRepository.countUserByCreatedAtBetween(start(today), end(today)).toString();
        String todayLoginCount = usageEventRepository.countByEventTypeAndOccurredAtBetween(EventType.LOGIN, start(today), end(today)).toString();
        String todayNonSignUpUser = tripPlanRepository.countByUserIsNullAndCreatedAtBetween(start(today), end(today)).toString();
        String todaySignUpUser = tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(start(today), end(today)).toString();

        return new StatsSeriesResponse("접속자 통계", List.of(
                new StatsSeriesResponse.Point("오늘 방문 수", todayVisitorCount),
                new StatsSeriesResponse.Point("회원가입 수", todaySignUpCount),
                new StatsSeriesResponse.Point("로그인 수", todayLoginCount),
                new StatsSeriesResponse.Point("회원 이용 수", todaySignUpUser),
                new StatsSeriesResponse.Point("비회원 이용 수", todayNonSignUpUser)
            )
        );
    }

    public StatsSeriesResponse usage() {
        LocalDate today = LocalDate.now();

        String totalTripPlanCount = Long.valueOf(tripPlanRepository.count()).toString();
        String todayTripPlanCount = Long.valueOf(tripPlanRepository.countTripPlanByCreatedAtBetween(start(today), end(today))).toString();
        String todayTopContent = getContentTitle(today, today);
        String todayTopSpot = getSpotName(today, today);
        String todaySave = usageEventRepository.countByEventTypeAndOccurredAtBetween(EventType.TRIP_SAVED, start(today), end(today)).toString();
        String todayShare = usageEventRepository.countByEventTypeAndOccurredAtBetween(EventType.TRIP_SHARED, start(today), end(today)).toString();
        String todayTripGen = usageEventRepository.countByEventTypeAndOccurredAtBetween(EventType.TRIP_GENERATED, start(today), end(today)).toString();
        String todayTripReGen = usageEventRepository.countByEventTypeAndOccurredAtBetween(EventType.TRIP_REGENERATED, start(today), end(today)).toString();

        return new StatsSeriesResponse("서비스 이용 통계", List.of(
                new StatsSeriesResponse.Point("총 일정 생성 수", totalTripPlanCount),
                new StatsSeriesResponse.Point("오늘 일정 생성 수", todayTripPlanCount),
                new StatsSeriesResponse.Point("오늘 AI 생성 수",todayTripGen),
                new StatsSeriesResponse.Point("오늘 AI  재생성 수",todayTripReGen),
                new StatsSeriesResponse.Point("인기 작품", todayTopContent),
                new StatsSeriesResponse.Point("인기 성지", todayTopSpot),
                new StatsSeriesResponse.Point("저장 수", todaySave),
                new StatsSeriesResponse.Point("공유 수", todayShare)
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
                throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
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

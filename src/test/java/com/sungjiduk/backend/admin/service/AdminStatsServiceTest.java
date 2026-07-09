package com.sungjiduk.backend.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.common.config.SecurityConfig;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.event.entity.EventType;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.trip.repository.TripStopRepository;
import com.sungjiduk.backend.user.repository.UserRepository;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SpringBootTest
@DisplayName("AdminStats")
@Import(SecurityConfig.class)
public class AdminStatsServiceTest {
    @Autowired
    AdminStatsService adminStatsService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    UsageEventRepository usageEventRepository;

    @MockitoBean
    TripPlanRepository tripPlanRepository;

    @MockitoBean
    TripStopRepository tripStopRepository;

    @MockitoBean
    PilgrimageSpotRepository pilgrimageSpotRepository;

    @MockitoBean
    ContentRepository contentRepository;

    @Nested
    @DisplayName("overview는")
    class overview {
        @Nested
        @DisplayName("다음과 같은 상황에서 성공해야한다.")
        class overview_success {
            @Test
            @DisplayName("1. 일일 이용자가 한명도 없을 경우")
            void overview_no_usage()
            {
                // given
                given(userRepository.count()).willReturn(0L); // userTotalCount
                given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L); // todayVisitorCount
                given(tripPlanRepository.count()).willReturn(0L); // todayTripPlanCount
                given(tripPlanRepository.findMostFrequentContent(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class))).willReturn(new ArrayList<>()); // getContentTitle
                given(tripStopRepository.findMostFrequentSpotToday(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class))).willReturn(new ArrayList<>()); // getSpotName

                // when
                AdminStatsOverviewResponse response = adminStatsService.overview();

                // then
                assertThat(Objects.equals(response.topContent(), "아직 집계된 작품이 없습니다."));
                assertThat(Objects.equals(response.topSpot(), "아직 집계된 성지가 없습니다"));
            }

            @Test
            @DisplayName("2. 이용객이 발생 한 경우")
            void overview_normal() {
                // given
                given(userRepository.count()).willReturn(1L);

                given(usageEventRepository.countUsageEventByOccurredAtBetween(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class))
                ).willReturn(1L);

                given(tripPlanRepository.count()).willReturn(1L);

                given(tripPlanRepository.findMostFrequentContent(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of(
                        Content.builder()
                            .title("콜 오브 듀티 모던 워페어 2")
                            .category("GAME")
                            .country("미국")
                            .description("망겜")
                            .build()
                        )
                    )
                );

                given(tripStopRepository.findMostFrequentSpotToday(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of(1L)));

                given(pilgrimageSpotRepository.findById(any(Long.class))).willReturn(
                    Optional.ofNullable(PilgrimageSpot.builder()
                        .content(null)
                        .name("굴라그")
                        .address("러시아")
                        .lat(new BigDecimal(1))
                        .lng(new BigDecimal(1))
                        .city("캄차카 반도")
                        .recommendedDurationMin(1)
                        .referenceUrl("www.callofduty.com")
                        .build())
                );

                // when
                AdminStatsOverviewResponse response = adminStatsService.overview();

                // then
                assertThat(!Objects.equals(response.topContent(), "아직 집계된 작품이 없습니다."));
                assertThat(!Objects.equals(response.topSpot(), "아직 집계된 성지가 없습니다"));
            }
        }

        @Nested
        @DisplayName("다음과 같은 경우에는 실패해야 한다.")
        class overview_failed {
            @Test
            @DisplayName("1. 등록되지 않는 성지가 잡히는 경우")
            void overview_no_content() {
                // given
                given(userRepository.count()).willReturn(1L);

                given(usageEventRepository.countUsageEventByOccurredAtBetween(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class))
                ).willReturn(1L);

                given(tripPlanRepository.count()).willReturn(1L);

                given(tripPlanRepository.findMostFrequentContent(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of(
                        Content.builder()
                    .title("라스트 오브 어스 파트 2")
                    .category("GAME")
                    .country("미국")
                    .description("아직 출시되지 않았습니다.")
                    .build()
                )));

                given(tripStopRepository.findMostFrequentSpotToday(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of(1L)));

                given(pilgrimageSpotRepository.findById(any(Long.class))).willReturn(Optional.empty());

                // when
                try {
                    adminStatsService.overview();
                    // then
                } catch (BusinessException e) {
                    return;
                }

                // 예외가 발생하지 않으면 테스트 실패
                assertThat(true);
            }
        }
    }

    @Nested
    @DisplayName("visitors는")
    class visitor {
        @Test
        @DisplayName("정상적으로 조회가 완료되면 StatsSeriesResponse를 반환해야 한다")
        void success() {
            //given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L);
            given(userRepository.countUserByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L);
            given(usageEventRepository.countByEventTypeAndOccurredAtBetween(any(EventType.class), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L);
            given(tripPlanRepository.countByUserIsNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L);
            given(tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L);

            // when
            StatsSeriesResponse response = adminStatsService.visitors();

            //then
            assertThat(response.points().size() == 5);
        }
    }

    @Nested
    @DisplayName("usage는")
    class usage {
        @Test
        @DisplayName("정상적으로 조회가 되면 StatsSeriesResponse를 반환해야 한다")
        void success() {
            // given
            given(tripPlanRepository.count()).willReturn(1L); // totalTripPlanCount

            given(tripPlanRepository.countTripPlanByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L); // todayTripPlanCount

            given(tripPlanRepository.findMostFrequentContent(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class))).willReturn(new ArrayList<>(List.of(
                    Content.builder()
                        .title("사이버펑크 엣지러너")
                        .category("ANIME")
                        .country("미국")
                        .description("사이버 사이코들이 아라사카에게 참교육 당하는 내용의 애니메이션")
                        .build()
                )
                )
            ); // todayTopContent

            given(usageEventRepository.countByEventTypeAndOccurredAtBetween(any(EventType.class),
                any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L); // save, share, gen, regen

            given(pilgrimageSpotRepository.findById(any(Long.class))).willReturn(
                Optional.ofNullable(PilgrimageSpot.builder()
                    .content(null)
                    .name("아라사카 타워")
                    .address("미국")
                    .lat(new BigDecimal(1))
                    .lng(new BigDecimal(1))
                    .city("나이트 시티")
                    .recommendedDurationMin(1)
                    .referenceUrl("www.cyberpunk.net")
                    .build())
            );

            // when
            StatsSeriesResponse response = adminStatsService.usage();

            // then
            assertThat(response.points().size() == 8);
        }
    }
}

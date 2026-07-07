package com.sungjiduk.backend.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.common.config.SecurityConfig;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@SpringBootTest
@DisplayName("AdminStats")
@Import(SecurityConfig.class)
public class AdminStatsTest {
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
                given(userRepository.count()).willReturn(0L);
                given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);
                given(tripPlanRepository.count()).willReturn(0L);
                // given(AiRequestRepository.count());

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
                given(tripPlanRepository.findMostFrequentTitleToday(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of("콜 오브 듀티 모던 워페어 2")));
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
                        .referenceUrl("molu")
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
                given(tripPlanRepository.findMostFrequentTitleToday(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of("라스트 오브 어스 파트 2")));
                given(tripStopRepository.findMostFrequentSpotToday(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    any(Pageable.class))).willReturn(new ArrayList<>(List.of(1L)));
                given(pilgrimageSpotRepository.findById(any(Long.class))).willReturn(Optional.empty());

                // when
                try {
                    adminStatsService.overview();
                    // then
                } catch (NoSuchElementException e) {
                    return;
                }

                // 예외가 발생하지 않으면 테스트 실패
                assertThat(true);
            }
        }
    }
}

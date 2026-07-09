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

import com.sungjiduk.backend.admin.dto.request.AdminMajorUserRateRequest;
import com.sungjiduk.backend.admin.dto.request.Duration;
import com.sungjiduk.backend.admin.dto.response.AdminMajorUserResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@DisplayName("AdminContentService")
public class AdminMajorServiceTest {
    @Autowired
    AdminMajorService adminMajorService;

    @MockitoBean
    PilgrimageSpotRepository pilgrimageSpotRepository;
    @MockitoBean
    TripStopRepository tripStopRepository;
    @MockitoBean
    TripPlanRepository tripPlanRepository;
    @MockitoBean
    UsageEventRepository usageEventRepository;

    @Nested
    @DisplayName("user는")
    class user {
        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {
            // given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.week, LocalDate.of(2025, 7, 1)));

            // then
            assertThat(response.values().size() == 7);
            assertThat(response.dates().size() == 7);
        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {
            // given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.month, LocalDate.of(2026, 7, 1)));

            // then
            assertThat(response.values().size() == 4).isTrue();
            assertThat(response.dates().size() == 4).isTrue();

            String []expected = {"2026-06-2", "2026-06-3", "2026-06-4", "2026-06-5"};

            for (int i = 0; i < 4; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {
            // given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.year, LocalDate.of(2026, 7, 1)));

            // then
            assertThat(response.values().size() == 12).isTrue();
            assertThat(response.dates().size() == 12).isTrue();

            String []expected = {"2025-08", "2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"};

            for (int i = 0; i < 12; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("rate는")
    class rate {
        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {

        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {

        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {

        }
    }

    @Nested
    @DisplayName("content는")
    class content {
        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {

        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {

        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {

        }

        @Test
        @DisplayName("count보다 적은 작품수가 주어지면 기타 통계가 잡히지 않아야 한다.")
        void success_small() {

        }
    }

    @Nested
    @DisplayName("spot은")
    class spot {
        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {

        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {

        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {

        }

        @Test
        @DisplayName("count보다 적은 작품수가 주어지면 기타 통계가 잡히지 않아야 한다.")
        void success_small() {

        }
    }
}

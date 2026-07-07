package com.sungjiduk.backend.trip.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

@Sql("classpath:fixtures/admin/test_data/findMostFrequentContentIdToday_test_data.sql")
@DisplayName("TripPlanRepository")
@SpringBootTest
public class TripPlanRepositoryTest {
    @Nested
    @DisplayName("findMostFrequentContentIdToday는")
    class findMostFrequentContentIdToday {
        @Autowired
        TripPlanRepository tripPlanRepository;

        @Test
        @DisplayName("특정 날짜 기준으로 가장 많이 등록된 작품의 id값을 가진 리스트를 반환해야 한다.")
        void test() {
            // given
            LocalDate localDate = LocalDate.of(2026, 7, 4);

            // when
            List<Long> mostFrequentSpotToday = tripPlanRepository.findMostFrequentContentIdToday(
                start(localDate),
                end(localDate),
                PageRequest.of(0, 1)
            );

            // then
            assertThat(mostFrequentSpotToday.getFirst() == 5);
        }

        private LocalDateTime start(LocalDate time) {
            return time.atStartOfDay(); // 0시 0분 0초
        }

        private LocalDateTime end(LocalDate time) {
            return time.atTime(LocalTime.MAX); // 23시 59분 59.99999...초
        }
    }
}

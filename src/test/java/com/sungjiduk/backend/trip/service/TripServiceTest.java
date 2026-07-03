package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.entity.SpotType;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.entity.TripStop;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TripServiceTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripPlanRepository tripPlanRepository;

    @Test
    void generate_persists_draft_trip_plan() {
        TripGenerateRequest request = new TripGenerateRequest(
                1L, 3, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                List.of(1L, 2L, 3L), List.of());

        TripResponse response = tripService.generate(request);

        assertThat(response.tripId()).isNotNull();
        TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TripStatus.DRAFT);
        assertThat(saved.getContentId()).isEqualTo(1L);
        assertThat(saved.getDurationDays()).isEqualTo(3);
    }

    @Test
    void generate_distributes_selected_spots_across_days_as_pilgrimage_stops() {
        TripGenerateRequest request = new TripGenerateRequest(
                1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                List.of(10L, 20L, 30L, 40L), List.of());

        TripResponse response = tripService.generate(request);

        TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
        assertThat(saved.getDays()).hasSize(2);

        List<TripStop> allStops = saved.getDays().stream()
                .flatMap(day -> day.getStops().stream())
                .toList();
        assertThat(allStops).allMatch(stop -> stop.getSpotType() == SpotType.PILGRIMAGE);
        assertThat(allStops).extracting(TripStop::getPilgrimageSpotId)
                .containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
    }

    @Test
    void generate_excludes_excluded_spot_ids() {
        TripGenerateRequest request = new TripGenerateRequest(
                1L, 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                List.of(10L, 20L, 30L), List.of(20L));

        TripResponse response = tripService.generate(request);

        TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
        List<Long> spotIds = saved.getDays().stream()
                .flatMap(day -> day.getStops().stream())
                .map(TripStop::getPilgrimageSpotId)
                .toList();
        assertThat(spotIds).containsExactlyInAnyOrder(10L, 30L);
    }

    @Test
    void generate_response_reflects_generated_days_and_stops() {
        TripGenerateRequest request = new TripGenerateRequest(
                1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                List.of(10L, 20L, 30L, 40L), List.of());

        TripResponse response = tripService.generate(request);

        assertThat(response.days()).hasSize(2);
        List<Long> spotIds = response.days().stream()
                .flatMap(day -> day.stops().stream())
                .map(TripResponse.Stop::spotId)
                .toList();
        assertThat(spotIds).containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
    }

    @Test
    void save_marks_draft_plan_as_saved() {
        TripPlan draft = tripPlanRepository.save(TripPlan.builder()
                .contentId(1L)
                .durationDays(2)
                .title("성지순례 2일 루트")
                .status(TripStatus.DRAFT)
                .build());

        TripSummaryResponse response = tripService.save(draft.getId());

        assertThat(response.tripId()).isEqualTo(draft.getId());
        assertThat(response.durationDays()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("SAVED");
        assertThat(tripPlanRepository.findById(draft.getId()).orElseThrow().getStatus())
                .isEqualTo(TripStatus.SAVED);
    }

    @Test
    void save_throws_when_trip_not_found() {
        assertThatThrownBy(() -> tripService.save(999L))
                .isInstanceOf(TripNotFoundException.class);
    }
}

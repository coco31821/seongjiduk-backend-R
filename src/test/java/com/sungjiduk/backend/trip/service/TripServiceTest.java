package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.ailog.entity.AiRequestStatus;
import com.sungjiduk.backend.ailog.entity.AiRequestType;
import com.sungjiduk.backend.ailog.repository.AiRequestLogRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.content.service.ContentService;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.AiDescribeClient;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import com.sungjiduk.backend.spot.service.RouteVerificationService;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.trip.infra.dto.AiTripRequest;
import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.entity.SpotType;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.entity.TripStop;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.infra.AiTripClient;
import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.attraction.repository.NearbyAttractionRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest
@Transactional
@DisplayName("TripService")
class TripServiceTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripPlanRepository tripPlanRepository;

    @Autowired
    private NearbyAttractionRepository attractionRepository;

    @Autowired
    private AiRequestLogRepository aiRequestLogRepository;

    @Autowired
    private UserRepository userRepository;

    private User member;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @Autowired
    private ContentService contentService;

    @MockitoBean
    private AiDescribeClient aiDescribeClient;

    @MockitoBean
    private RouteVerificationService routeVerificationService;

    @MockitoBean
    private ReverseGeocoder reverseGeocoder;

    // TripPlan.content가 FK 필수라 테스트마다 실제 작품 행을 만들어 쓴다.
    private Content content;

    // 기본은 ai-service 미가용 → 로컬 폴백 경로를 결정론적으로 검증한다.
    // AI 성공 경로 테스트에서만 willReturn으로 재정의한다.
    @MockitoBean
    private AiTripClient aiTripClient;

    @BeforeEach
    void setUp() {
        willThrow(new RuntimeException("ai-service down")).given(aiTripClient).generate(any());
        willThrow(new RuntimeException("describe down")).given(aiDescribeClient).describe(any());
        content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "러브라이브! 설명"));
        member = userRepository.save(User.builder()
                .email("fan@example.com").passwordHash("hash").nickname("muse_fan").build());
    }

    private TripGenerateRequest defaultRequest() {
        return new TripGenerateRequest(
                content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                List.of(10L), List.of(), null, null, null);
    }

    @Nested
    @DisplayName("관광지를 담아 generate하면")
    class GenerateWithAttractions {

        private TripGenerateRequest requestWith(java.util.List<TripGenerateRequest.AttractionInput> attractions) {
            return new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo", "PILGRIMAGE_ONLY",
                    java.util.List.of(10L), java.util.List.of(), attractions, null, null);
        }

        @Test
        @DisplayName("관광지를 mapsUrl 멱등으로 영속화하고 ATTRACTION stop으로 배치한다")
        void persistsAttractionsAndAddsStops() {
            // given
            var attractions = java.util.List.of(
                    new TripGenerateRequest.AttractionInput("간다 신사", "신사", 35.702, 139.768, "https://maps/kanda"));

            // when — 같은 관광지로 두 번 생성해도 한 행만 생긴다
            TripResponse first = tripService.generate(null, requestWith(attractions));
            tripService.generate(null, requestWith(attractions));

            // then
            assertThat(attractionRepository.findAll()).hasSize(1);
            var stops = first.days().stream().flatMap(d -> d.stops().stream()).toList();
            assertThat(stops).anyMatch(stop -> "ATTRACTION".equals(stop.spotType())
                    && "간다 신사".equals(stop.name()));
        }

        @Test
        @DisplayName("재생성 시 selectedAttractionIds로 기존 관광지를 유지한다")
        void regenerateKeepsAttractionsByIds() {
            // given
            TripResponse created = tripService.generate(null, requestWith(java.util.List.of(
                    new TripGenerateRequest.AttractionInput("간다 신사", "신사", 35.702, 139.768, "https://maps/kanda"))));
            Long attractionId = attractionRepository.findAll().get(0).getId();

            // when
            TripResponse regenerated = tripService.regenerate(null, created.tripId(), new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo", "PILGRIMAGE_ONLY",
                    java.util.List.of(10L), java.util.List.of(), null, java.util.List.of(attractionId), null));

            // then
            var stops = regenerated.days().stream().flatMap(d -> d.stops().stream()).toList();
            assertThat(stops).anyMatch(stop -> "ATTRACTION".equals(stop.spotType())
                    && "간다 신사".equals(stop.name()));
        }
    }

    @Nested
    @DisplayName("generate는")
    class Generate {

        @Test
        @DisplayName("일정을 DRAFT 상태로 저장한다")
        void savesDraftPlan() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 3, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(1L, 2L, 3L), List.of(), null, null, null);

            // when
            TripResponse response = tripService.generate(null, request);

            // then
            assertThat(response.tripId()).isNotNull();
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(TripStatus.DRAFT);
            assertThat(saved.getContent().getId()).isEqualTo(content.getId());
            assertThat(saved.getDurationDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("선택 스팟을 일자별 PILGRIMAGE stop으로 분배한다")
        void distributesSelectedSpotsAcrossDays() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L, 40L), List.of(), null, null, null);

            // when
            TripResponse response = tripService.generate(null, request);

            // then
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
        @DisplayName("제외 스팟은 일정에서 뺀다")
        void excludesExcludedSpotIds() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L), List.of(20L), null, null, null);

            // when
            TripResponse response = tripService.generate(null, request);

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            List<Long> spotIds = saved.getDays().stream()
                    .flatMap(day -> day.getStops().stream())
                    .map(TripStop::getPilgrimageSpotId)
                    .toList();
            assertThat(spotIds).containsExactlyInAnyOrder(10L, 30L);
        }

        @Test
        @DisplayName("응답에 생성된 Day와 stop을 담아 반환한다")
        void responseReflectsGeneratedDaysAndStops() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L, 40L), List.of(), null, null, null);

            // when
            TripResponse response = tripService.generate(null, request);

            // then
            assertThat(response.days()).hasSize(2);
            List<Long> spotIds = response.days().stream()
                    .flatMap(day -> day.stops().stream())
                    .map(TripResponse.Stop::spotId)
                    .toList();
            assertThat(spotIds).containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
        }
    }

    @Nested
    @DisplayName("findTrip은")
    class FindTrip {

        @Test
        @DisplayName("저장된 일정의 Day와 stop을 담아 반환한다")
        void returnsTripDetail() {
            // given
            TripResponse created = tripService.generate(null, new TripGenerateRequest(
                    content.getId(), 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L, 40L), List.of(), null, null, null));

            // when
            TripResponse found = tripService.findTrip(created.tripId());

            // then
            assertThat(found.tripId()).isEqualTo(created.tripId());
            assertThat(found.days()).hasSize(2);
            List<Long> spotIds = found.days().stream()
                    .flatMap(day -> day.stops().stream())
                    .map(TripResponse.Stop::spotId)
                    .toList();
            assertThat(spotIds).containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.findTrip(missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findMyTrips는")
    class FindMyTrips {

        @Test
        @DisplayName("저장된 모든 일정을 요약으로 반환한다")
        void returnsTripSummaries() {
            // given
            tripPlanRepository.save(TripPlan.builder()
                    .user(member).content(content).durationDays(2).title("뮤즈 2일 루트").status(TripStatus.SAVED).build());
            tripPlanRepository.save(TripPlan.builder()
                    .user(member).content(content).durationDays(3).title("뮤즈 3일 루트").status(TripStatus.DRAFT).build());

            // when
            List<TripSummaryResponse> result = tripService.findMyTrips(member.getId());

            // then
            assertThat(result).extracting(TripSummaryResponse::title)
                    .containsExactlyInAnyOrder("뮤즈 2일 루트", "뮤즈 3일 루트");
            assertThat(result).extracting(TripSummaryResponse::status)
                    .containsExactlyInAnyOrder("SAVED", "DRAFT");
        }

        @Test
        @DisplayName("일정이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNone() {
            // when
            List<TripSummaryResponse> result = tripService.findMyTrips(member.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save는")
    class Save {

        @Test
        @DisplayName("DRAFT 일정을 SAVED로 전이한다")
        void marksDraftPlanAsSaved() {
            // given
            TripPlan draft = tripPlanRepository.save(TripPlan.builder()
                    .user(member).content(content)
                    .durationDays(2)
                    .title("성지순례 2일 루트")
                    .status(TripStatus.DRAFT)
                    .build());

            // when
            TripSummaryResponse response = tripService.save(member.getId(), draft.getId());

            // then
            assertThat(response.tripId()).isEqualTo(draft.getId());
            assertThat(response.durationDays()).isEqualTo(2);
            assertThat(response.status()).isEqualTo("SAVED");
            assertThat(tripPlanRepository.findById(draft.getId()).orElseThrow().getStatus())
                    .isEqualTo(TripStatus.SAVED);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.save(member.getId(), missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete는")
    class Delete {

        @Test
        @DisplayName("저장된 일정을 삭제한다")
        void deletesTrip() {
            // given
            TripPlan plan = tripPlanRepository.save(TripPlan.builder()
                    .user(member).content(content)
                    .durationDays(2)
                    .title("성지순례 2일 루트")
                    .status(TripStatus.SAVED)
                    .build());

            // when
            tripService.delete(member.getId(), plan.getId());

            // then
            assertThat(tripPlanRepository.findById(plan.getId())).isEmpty();
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.delete(member.getId(), missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("share는")
    class Share {

        @Test
        @DisplayName("일정에 shareToken을 발급하고 공유 응답을 반환한다")
        void issuesShareTokenAndReturnsResponse() {
            // given
            TripPlan plan = tripPlanRepository.save(TripPlan.builder()
                    .user(member).content(content).durationDays(2).title("뮤즈 2일 루트").status(TripStatus.SAVED).build());

            // when
            TripShareResponse response = tripService.share(member.getId(), plan.getId());

            // then
            assertThat(response.tripId()).isEqualTo(plan.getId());
            String token = tripPlanRepository.findById(plan.getId()).orElseThrow().getShareToken();
            assertThat(token).isNotBlank();
            assertThat(response.shareUrl()).contains(token);
        }

        @Test
        @DisplayName("같은 일정을 다시 공유해도 토큰이 유지된다")
        void keepsSameTokenOnReshare() {
            // given
            TripPlan plan = tripPlanRepository.save(TripPlan.builder()
                    .user(member).content(content).durationDays(2).title("뮤즈 2일 루트").status(TripStatus.SAVED).build());

            // when
            tripService.share(member.getId(), plan.getId());
            String firstToken = tripPlanRepository.findById(plan.getId()).orElseThrow().getShareToken();
            tripService.share(member.getId(), plan.getId());
            String secondToken = tripPlanRepository.findById(plan.getId()).orElseThrow().getShareToken();

            // then
            assertThat(firstToken).isNotBlank();
            assertThat(secondToken).isEqualTo(firstToken);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.share(member.getId(), missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("regenerate는")
    class Regenerate {

        @Test
        @DisplayName("기존 일정에 추가/제외 스팟을 반영해 재배치한다")
        void reflectsAddedAndExcludedSpots() {
            // given
            TripResponse created = tripService.generate(null, new TripGenerateRequest(
                    content.getId(), 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L), List.of(), null, null, null));

            // when — 30 추가, 20 제외
            TripResponse result = tripService.regenerate(null, created.tripId(), new TripGenerateRequest(
                    content.getId(), 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L), List.of(20L), null, null, null));

            // then
            assertThat(result.tripId()).isEqualTo(created.tripId());
            List<Long> responseSpotIds = result.days().stream()
                    .flatMap(day -> day.stops().stream())
                    .map(TripResponse.Stop::spotId)
                    .toList();
            assertThat(responseSpotIds).containsExactlyInAnyOrder(10L, 30L);

            List<Long> persistedSpotIds = tripPlanRepository.findById(created.tripId()).orElseThrow()
                    .getDays().stream()
                    .flatMap(day -> day.getStops().stream())
                    .map(TripStop::getPilgrimageSpotId)
                    .toList();
            assertThat(persistedSpotIds).containsExactlyInAnyOrder(10L, 30L);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null);

            // when / then
            assertThatThrownBy(() -> tripService.regenerate(null, 999L, request))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("AI 체류분이 캐시에 있으면")
    class AiStayMinutes {

        @Test
        @DisplayName("일정 stop의 stayMinutes에 엔티티 기본값 대신 AI 추정 분을 쓴다")
        void usesCachedAiMinutesOverEntityDefault() {
            // given — describe 캐시에 45분이 적재된 성지 (엔티티 값은 30분)
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "とんかつ屋さん", "東京都千代田区",
                    new java.math.BigDecimal("35.7020000"), new java.math.BigDecimal("139.7680000"),
                    "千代田区", 30, null));
            org.mockito.BDDMockito.willReturn(new AiDescribeResult(
                    content.getId(), "openai",
                    List.of(new AiDescribeResult.AiSpotDescription(
                            spot.getId(), "9화의 돈카츠 가게", "실제 모델 식당", "돈카츠야상", 45, List.of()))))
                    .given(aiDescribeClient).describe(any());
            contentService.findContentSpots(content.getId()); // 캐시 적재 (프리웜과 동일 경로)

            // when — ai-service 미가용 → 로컬 폴백 배치
            TripResponse response = tripService.generate(null, new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(spot.getId()), List.of(), null, null, null));

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            TripStop stop = saved.getDays().get(0).getStops().get(0);
            assertThat(stop.getStayMinutes()).isEqualTo(45);
        }
    }

    @Nested
    @DisplayName("검증 코스 캐시가 있으면")
    class VerifiedCourseBackbone {

        @Test
        @DisplayName("AI 일정 요청에 verifiedCourses로 전달한다")
        void passesCachedCoursesToAiRequest() {
            // given
            org.mockito.BDDMockito.given(routeVerificationService.cachedCourseSpotIds(content.getId()))
                    .willReturn(List.of(List.of(5L, 6L)));

            // when — ai 미가용이라 폴백하지만 요청 자체는 보낸다
            tripService.generate(null, new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null));

            // then
            var captor = org.mockito.ArgumentCaptor.forClass(AiTripRequest.class);
            org.mockito.BDDMockito.then(aiTripClient).should().generate(captor.capture());
            assertThat(captor.getValue().verifiedCourses()).containsExactly(List.of(5L, 6L));
        }
    }

    @Nested
    @DisplayName("AI 로그와 일정 삭제는")
    class AiLogSurvivesDeletion {

        @Test
        @DisplayName("일정을 삭제해도 AI 호출 로그는 남는다 (통계 보존 — FK가 삭제를 막으면 안 됨)")
        void deletingTripKeepsAiLogs() {
            // given
            TripResponse created = tripService.generate(member.getId(), defaultRequest());

            // when
            tripService.delete(member.getId(), created.tripId());

            // then — 일정은 사라지고 로그는 tripPlanId로 남는다
            assertThat(tripPlanRepository.findById(created.tripId())).isEmpty();
            var logs = aiRequestLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getTripPlanId()).isEqualTo(created.tripId());
        }
    }

    @Nested
    @DisplayName("출발지 앵커는")
    class StartAnchor {

        @Test
        @DisplayName("startLocation을 지오코딩해 AI 요청에 좌표로 전달하고, 같은 문자열은 캐시한다")
        void forwardsGeocodedStartToAiRequest() {
            // given — 싱글턴 캐시 오염 방지를 위해 이 스펙 전용 출발지 사용
            org.mockito.BDDMockito.given(reverseGeocoder.forward("Ueno Station"))
                    .willReturn(java.util.Optional.of(new ReverseGeocoder.LatLng(35.6812, 139.7671)));
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Ueno Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null);

            // when — 두 번 생성해도 지오코딩은 1회
            tripService.generate(null, request);
            tripService.generate(null, request);

            // then
            var captor = org.mockito.ArgumentCaptor.forClass(AiTripRequest.class);
            org.mockito.BDDMockito.then(aiTripClient).should(org.mockito.Mockito.times(2)).generate(captor.capture());
            assertThat(captor.getValue().startLat()).isEqualTo(35.6812);
            assertThat(captor.getValue().startLng()).isEqualTo(139.7671);
            org.mockito.BDDMockito.then(reverseGeocoder).should(org.mockito.Mockito.times(1)).forward("Ueno Station");
        }

        @Test
        @DisplayName("지오코딩 실패·빈 출발지는 좌표 없이 보낸다 (기존 동작)")
        void sendsNullWhenGeocodeFails() {
            // given
            org.mockito.BDDMockito.given(reverseGeocoder.forward(anyString()))
                    .willReturn(java.util.Optional.empty());

            // when
            tripService.generate(null, defaultRequest());

            // then
            var captor = org.mockito.ArgumentCaptor.forClass(AiTripRequest.class);
            org.mockito.BDDMockito.then(aiTripClient).should().generate(captor.capture());
            assertThat(captor.getValue().startLat()).isNull();
        }
    }

    @Nested
    @DisplayName("Trip 소유권은")
    class TripOwnership {

        @Test
        @DisplayName("로그인 사용자의 generate는 플랜과 AI 로그에 소유자를 세팅한다")
        void generateSetsOwnerForAuthenticatedUser() {
            // when
            TripResponse response = tripService.generate(member.getId(), defaultRequest());

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            assertThat(saved.getUser().getId()).isEqualTo(member.getId());
            assertThat(aiRequestLogRepository.findAll().get(0).getUser().getId()).isEqualTo(member.getId());
        }

        @Test
        @DisplayName("비회원 generate는 소유자 없이 저장된다 (기존 동작 유지)")
        void anonymousGenerateStaysUnowned() {
            // when
            TripResponse response = tripService.generate(null, defaultRequest());

            // then
            assertThat(tripPlanRepository.findById(response.tripId()).orElseThrow().isUnowned()).isTrue();
        }

        @Test
        @DisplayName("findMyTrips는 내 일정만 반환한다")
        void findMyTripsFiltersByOwner() {
            // given
            User other = userRepository.save(User.builder()
                    .email("other@example.com").passwordHash("hash").nickname("other").build());
            tripService.generate(member.getId(), defaultRequest());
            tripService.generate(other.getId(), defaultRequest());
            tripService.generate(null, defaultRequest()); // 비회원 플랜

            // when
            List<TripSummaryResponse> mine = tripService.findMyTrips(member.getId());

            // then
            assertThat(mine).hasSize(1);
        }

        @Test
        @DisplayName("save는 비회원 생성 플랜의 소유권을 가져온다 (로그인 후 이어서 저장 플로우)")
        void saveClaimsUnownedPlan() {
            // given
            TripResponse created = tripService.generate(null, defaultRequest());

            // when
            tripService.save(member.getId(), created.tripId());

            // then
            assertThat(tripPlanRepository.findById(created.tripId()).orElseThrow()
                    .isOwnedBy(member.getId())).isTrue();
        }

        @Test
        @DisplayName("남의 플랜은 save·delete·share·regenerate 전부 FORBIDDEN")
        void forbidsActionsOnOthersPlan() {
            // given
            User other = userRepository.save(User.builder()
                    .email("other@example.com").passwordHash("hash").nickname("other").build());
            TripResponse created = tripService.generate(other.getId(), defaultRequest());

            // when / then
            assertThatThrownBy(() -> tripService.save(member.getId(), created.tripId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
            assertThatThrownBy(() -> tripService.delete(member.getId(), created.tripId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
            assertThatThrownBy(() -> tripService.share(member.getId(), created.tripId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
            assertThatThrownBy(() -> tripService.regenerate(member.getId(), created.tripId(), defaultRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("AI 호출 로그는")
    class AiRequestLogging {

        @Test
        @DisplayName("generate 시 TRIP_GENERATE 로그를 일정과 함께 남긴다 (폴백이면 FALLBACK)")
        void logsGenerateWithFallbackStatus() {
            // given — 기본 목킹: ai-service 다운 → 로컬 폴백 경로
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null);

            // when
            TripResponse response = tripService.generate(null, request);

            // then
            var logs = aiRequestLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getRequestType()).isEqualTo(AiRequestType.TRIP_GENERATE);
            assertThat(logs.get(0).getStatus()).isEqualTo(AiRequestStatus.FALLBACK);
            assertThat(logs.get(0).getTripPlanId()).isEqualTo(response.tripId());
            assertThat(logs.get(0).getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("ai-service가 성공하면 SUCCESS로 기록한다")
        void logsSuccessWhenAiResponds() {
            // given
            willReturn(new AiTripLayout(
                    "AI 제목",
                    List.of(new AiTripLayout.Day(1, "요약",
                            List.of(new AiTripLayout.Stop(1, "PILGRIMAGE", 10L, "spot",
                                    "10:00", 30, "이유")))),
                    "공유 문구", "openai")).given(aiTripClient).generate(any());
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null);

            // when
            tripService.generate(null, request);

            // then
            assertThat(aiRequestLogRepository.findAll())
                    .singleElement()
                    .extracting(log -> log.getStatus())
                    .isEqualTo(AiRequestStatus.SUCCESS);
        }

        @Test
        @DisplayName("regenerate 시 TRIP_REGENERATE 로그가 추가된다")
        void logsRegenerate() {
            // given
            TripResponse created = tripService.generate(null, new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null));

            // when
            tripService.regenerate(null, created.tripId(), new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L), List.of(), null, null, null));

            // then — generate 1건 + regenerate 1건
            var logs = aiRequestLogRepository.findAll();
            assertThat(logs).hasSize(2);
            assertThat(logs).extracting(log -> log.getRequestType())
                    .containsExactlyInAnyOrder(AiRequestType.TRIP_GENERATE, AiRequestType.TRIP_REGENERATE);
        }
    }

    @Nested
    @DisplayName("ai-service가 응답하면")
    class AiLayout {

        @Test
        @DisplayName("AI가 만든 제목·요약·이유·이름을 일정에 반영한다")
        void appliesAiTitleSummaryReasonAndName() {
            // given
            willReturn(new AiTripLayout(
                    "AI가 다듬은 뮤즈 성지순례",
                    List.of(new AiTripLayout.Day(1, "아키하바라 감성 산책",
                            List.of(new AiTripLayout.Stop(
                                    1, "PILGRIMAGE", 10L, "とんかつ屋さん",
                                    "10:00", 40, "9화 명장면의 무대라 놓칠 수 없어요.")))),
                    "AI가 만든 공유 문구",
                    "openai")).given(aiTripClient).generate(any());
            TripGenerateRequest request = new TripGenerateRequest(
                    content.getId(), 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of(), null, null, null);

            // when
            TripResponse response = tripService.generate(null, request);

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("AI가 다듬은 뮤즈 성지순례");
            assertThat(saved.getDays()).hasSize(1);
            assertThat(saved.getDays().get(0).getSummary()).isEqualTo("아키하바라 감성 산책");
            TripStop stop = saved.getDays().get(0).getStops().get(0);
            assertThat(stop.getPilgrimageSpotId()).isEqualTo(10L);
            assertThat(stop.getName()).isEqualTo("とんかつ屋さん");
            assertThat(stop.getReason()).isEqualTo("9화 명장면의 무대라 놓칠 수 없어요.");
            assertThat(response.days().get(0).stops().get(0).reason())
                    .isEqualTo("9화 명장면의 무대라 놓칠 수 없어요.");
        }
    }
}

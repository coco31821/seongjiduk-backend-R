package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.ailog.entity.AiRequestLog;
import com.sungjiduk.backend.ailog.entity.AiRequestStatus;
import com.sungjiduk.backend.ailog.entity.AiRequestType;
import com.sungjiduk.backend.ailog.repository.AiRequestLogRepository;
import com.sungjiduk.backend.attraction.entity.NearbyAttraction;
import com.sungjiduk.backend.attraction.repository.NearbyAttractionRepository;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.entity.SpotType;
import com.sungjiduk.backend.trip.entity.TripDay;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.entity.TripStop;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.infra.AiTripClient;
import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.trip.infra.dto.AiTripRequest;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripPlanRepository tripPlanRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final ContentRepository contentRepository;
    private final NearbyAttractionRepository attractionRepository;
    private final AiTripClient aiTripClient;
    private final com.sungjiduk.backend.content.service.ContentService contentService;
    private final com.sungjiduk.backend.spot.service.RouteVerificationService routeVerificationService;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final UserRepository userRepository;
    private final ReverseGeocoder reverseGeocoder;

    /** 출발지 문자열 → 좌표 캐시 (같은 출발지 반복 지오코딩 방지). 실패는 캐시하지 않아 일시 장애 후 재시도된다. */
    private final java.util.Map<String, java.util.Optional<ReverseGeocoder.LatLng>> startLocationCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public TripService(
            TripPlanRepository tripPlanRepository,
            PilgrimageSpotRepository spotRepository,
            ContentRepository contentRepository,
            NearbyAttractionRepository attractionRepository,
            AiTripClient aiTripClient,
            com.sungjiduk.backend.content.service.ContentService contentService,
            com.sungjiduk.backend.spot.service.RouteVerificationService routeVerificationService,
            AiRequestLogRepository aiRequestLogRepository,
            UserRepository userRepository,
            ReverseGeocoder reverseGeocoder
    ) {
        this.tripPlanRepository = tripPlanRepository;
        this.spotRepository = spotRepository;
        this.contentRepository = contentRepository;
        this.attractionRepository = attractionRepository;
        this.aiTripClient = aiTripClient;
        this.contentService = contentService;
        this.routeVerificationService = routeVerificationService;
        this.aiRequestLogRepository = aiRequestLogRepository;
        this.userRepository = userRepository;
        this.reverseGeocoder = reverseGeocoder;
    }

    /** 출발지 앵커 좌표 — 빈 문자열이면 지오코딩 없이 empty. */
    private java.util.Optional<ReverseGeocoder.LatLng> resolveStart(String startLocation) {
        if (startLocation == null || startLocation.isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.Optional<ReverseGeocoder.LatLng> cached = startLocationCache.get(startLocation);
        if (cached != null) {
            return cached;
        }
        java.util.Optional<ReverseGeocoder.LatLng> resolved = reverseGeocoder.forward(startLocation);
        if (resolved.isPresent()) {
            startLocationCache.put(startLocation, resolved);
        }
        return resolved;
    }

    /** userId가 있으면 User 프록시 참조 (조회 쿼리 없이 FK만) */
    private User userRef(Long userId) {
        return userId == null ? null : userRepository.getReferenceById(userId);
    }

    /** 소유자 가드 — 소유된 플랜은 소유자만 접근, 비회원 생성 플랜(무소유)은 통과 */
    private void requireAccess(TripPlan plan, Long userId) {
        if (!plan.isUnowned() && !plan.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /** AI 추정 체류분(describe 캐시) 우선, 없으면 엔티티 기본값 — 카드 표기와 일정을 일치시킨다. */
    private int stayMinutesFor(PilgrimageSpot spot) {
        return contentService.cachedRecommendedMinutes(spot.getId())
                .orElse(spot.getRecommendedDurationMin());
    }

    @Transactional
    public TripResponse generate(Long userId, TripGenerateRequest request) {
        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        TripPlan plan = TripPlan.builder()
                .user(userRef(userId))
                .content(content)
                .durationDays(request.durationDays())
                .startLocation(request.startLocation())
                .budgetLevel(request.budgetLevel())
                .travelStyle(request.travelStyle())
                .title("성지순례 " + request.durationDays() + "일 루트")
                .status(TripStatus.DRAFT)
                .build();

        AiRequestStatus aiStatus = layoutRoute(plan, request);

        TripPlan saved = tripPlanRepository.save(plan);
        recordAiRequest(saved, AiRequestType.TRIP_GENERATE, aiStatus, userId);
        return toResponse(saved);
    }

    /**
     * 일정 배치. ai-service(LangGraph)를 우선 호출하고, 실패하면 로컬 규칙으로 폴백한다.
     * 기존 Day는 비우고 다시 채우므로 generate/regenerate가 공유한다.
     */
    private AiRequestStatus layoutRoute(TripPlan plan, TripGenerateRequest request) {
        Map<Long, PilgrimageSpot> spotsById = loadCandidateSpots(request);
        List<NearbyAttraction> attractions = resolveAttractions(request);
        try {
            AiTripLayout layout = aiTripClient.generate(toAiRequest(plan, request, spotsById, attractions));
            applyAiLayout(plan, layout);
            return AiRequestStatus.SUCCESS;
        } catch (RuntimeException e) {
            log.warn("ai-service 일정 생성 실패, 로컬 배치로 폴백합니다: {}", e.getMessage());
            applyLocalLayout(plan, request, spotsById, attractions);
            return AiRequestStatus.FALLBACK;
        }
    }

    /**
     * AI 호출 로그 — 트랜잭션과 함께 기록(07 설계), 관리자 통계(C파트)가 읽기 전용 집계.
     */
    private void recordAiRequest(TripPlan plan, AiRequestType requestType, AiRequestStatus status, Long userId) {
        aiRequestLogRepository.save(AiRequestLog.builder()
                .user(userRef(userId))
                .tripPlanId(plan.getId())
                .requestType(requestType)
                .status(status)
                .build());
    }

    /** 새로 담은 관광지는 mapsUrl 멱등 upsert로 영속화하고, 재생성용 id들은 로드해 합친다. */
    private List<NearbyAttraction> resolveAttractions(TripGenerateRequest request) {
        Map<Long, NearbyAttraction> merged = new LinkedHashMap<>();
        if (request.attractions() != null) {
            for (TripGenerateRequest.AttractionInput input : request.attractions()) {
                if (input.mapsUrl() == null || input.mapsUrl().isBlank()) {
                    continue;
                }
                NearbyAttraction attraction = attractionRepository.findByMapsUrl(input.mapsUrl())
                        .orElseGet(() -> attractionRepository.save(NearbyAttraction.create(
                                input.name(), input.category(),
                                java.math.BigDecimal.valueOf(input.lat()),
                                java.math.BigDecimal.valueOf(input.lng()),
                                input.mapsUrl())));
                merged.put(attraction.getId(), attraction);
            }
        }
        if (request.selectedAttractionIds() != null && !request.selectedAttractionIds().isEmpty()) {
            attractionRepository.findAllById(request.selectedAttractionIds())
                    .forEach(a -> merged.putIfAbsent(a.getId(), a));
        }
        return new ArrayList<>(merged.values());
    }

    /** 선택 스팟(제외 제거)을 선택 순서대로 로드. 이름/도시는 AI 후보·로컬 이름 스냅샷에 쓴다. */
    private Map<Long, PilgrimageSpot> loadCandidateSpots(TripGenerateRequest request) {
        List<Long> selected = request.selectedSpotIds() == null ? List.of() : request.selectedSpotIds();
        Set<Long> excluded = request.excludedSpotIds() == null ? Set.of() : new HashSet<>(request.excludedSpotIds());

        Map<Long, PilgrimageSpot> loaded = new HashMap<>();
        for (PilgrimageSpot spot : spotRepository.findAllById(selected)) {
            loaded.put(spot.getId(), spot);
        }

        Map<Long, PilgrimageSpot> ordered = new LinkedHashMap<>();
        for (Long id : selected) {
            if (!excluded.contains(id)) {
                ordered.put(id, loaded.get(id)); // 아직 임포트 안 된 id면 null (이름 미상)
            }
        }
        return ordered;
    }

    private AiTripRequest toAiRequest(TripPlan plan, TripGenerateRequest request, Map<Long, PilgrimageSpot> spotsById,
                                      List<NearbyAttraction> attractions) {
        String title = contentRepository.findById(request.contentId())
                .map(content -> content.getTitle())
                .orElse(null);

        java.util.Optional<ReverseGeocoder.LatLng> start = resolveStart(request.startLocation());
        List<AiTripRequest.CandidateSpot> candidates = new ArrayList<>();
        spotsById.forEach((id, spot) -> {
            if (spot != null) {
                candidates.add(new AiTripRequest.CandidateSpot(
                        spot.getId(), spot.getName(), spot.getCity(),
                        spot.getLat() == null ? null : spot.getLat().doubleValue(),
                        spot.getLng() == null ? null : spot.getLng().doubleValue(),
                        stayMinutesFor(spot), "PILGRIMAGE"));
            }
        });
        for (NearbyAttraction attraction : attractions) {
            candidates.add(new AiTripRequest.CandidateSpot(
                    attraction.getId(), attraction.getName(), null,
                    attraction.getLat().doubleValue(), attraction.getLng().doubleValue(),
                    30, "ATTRACTION"));
        }

        return new AiTripRequest(
                new AiTripRequest.Content(request.contentId(), title),
                new AiTripRequest.Conditions(
                        request.durationDays(), request.budgetLevel(),
                        request.startLocation(), request.travelStyle()),
                candidates,
                new ArrayList<>(spotsById.keySet()),
                request.excludedSpotIds() == null ? List.of() : request.excludedSpotIds(),
                request.instruction(),
                routeVerificationService.cachedCourseSpotIds(request.contentId()),
                start.map(ReverseGeocoder.LatLng::lat).orElse(null),
                start.map(ReverseGeocoder.LatLng::lng).orElse(null));
    }

    private void applyAiLayout(TripPlan plan, AiTripLayout layout) {
        plan.changeTitle(layout.title());
        plan.getDays().clear();
        List<AiTripLayout.Day> days = layout.days() == null ? List.of() : layout.days();
        for (AiTripLayout.Day aiDay : days) {
            TripDay day = TripDay.builder().dayNo(aiDay.dayNo()).summary(aiDay.summary()).build();
            plan.addDay(day);
            List<AiTripLayout.Stop> stops = aiDay.stops() == null ? List.of() : aiDay.stops();
            for (AiTripLayout.Stop aiStop : stops) {
                SpotType spotType = parseSpotType(aiStop.spotType());
                day.addStop(TripStop.builder()
                        .spotType(spotType)
                        .pilgrimageSpotId(spotType == SpotType.PILGRIMAGE ? aiStop.spotId() : null)
                        .nearbyAttractionId(spotType == SpotType.ATTRACTION ? aiStop.spotId() : null)
                        .sequence(aiStop.sequence())
                        .name(aiStop.name())
                        .arrivalTime(aiStop.arrivalTime())
                        .stayMinutes(aiStop.stayMinutes())
                        .reason(aiStop.reason())
                        .build());
            }
        }
    }

    private SpotType parseSpotType(String raw) {
        try {
            return SpotType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return SpotType.PILGRIMAGE;
        }
    }

    /** ai-service 미가용 시 로컬 라운드로빈 배치(이름 스냅샷만 채우고 이유는 비운다). */
    private void applyLocalLayout(TripPlan plan, TripGenerateRequest request, Map<Long, PilgrimageSpot> spotsById,
                                  List<NearbyAttraction> attractions) {
        plan.getDays().clear();

        List<TripDay> days = new ArrayList<>();
        for (int dayNo = 1; dayNo <= plan.getDurationDays(); dayNo++) {
            TripDay day = TripDay.builder()
                    .dayNo(dayNo)
                    .summary("Day " + dayNo + " 성지순례")
                    .build();
            plan.addDay(day);
            days.add(day);
        }

        List<Long> spotIds = new ArrayList<>(spotsById.keySet());
        for (int i = 0; i < spotIds.size(); i++) {
            Long spotId = spotIds.get(i);
            PilgrimageSpot spot = spotsById.get(spotId);
            TripDay day = days.get(i % days.size());
            int sequence = day.getStops().size() + 1;
            day.addStop(TripStop.builder()
                    .spotType(SpotType.PILGRIMAGE)
                    .pilgrimageSpotId(spotId)
                    .sequence(sequence)
                    .name(spot != null ? spot.getName() : null)
                    .arrivalTime(String.format("%02d:00", 9 + sequence))
                    .stayMinutes(spot != null ? stayMinutesFor(spot) : 30)
                    .build());
        }
        for (int i = 0; i < attractions.size(); i++) {
            NearbyAttraction attraction = attractions.get(i);
            TripDay day = days.get((spotIds.size() + i) % days.size());
            int sequence = day.getStops().size() + 1;
            day.addStop(TripStop.builder()
                    .spotType(SpotType.ATTRACTION)
                    .nearbyAttractionId(attraction.getId())
                    .sequence(sequence)
                    .name(attraction.getName())
                    .arrivalTime(String.format("%02d:00", 9 + sequence))
                    .stayMinutes(30)
                    .build());
        }
    }

    private TripResponse toResponse(TripPlan plan) {
        List<TripResponse.DayPlan> days = plan.getDays().stream()
                .map(day -> new TripResponse.DayPlan(
                        day.getDayNo(),
                        day.getSummary(),
                        day.getStops().stream()
                                .map(stop -> new TripResponse.Stop(
                                        stop.getSequence(),
                                        stop.getSpotType().name(),
                                        stop.getPilgrimageSpotId() != null
                                                ? stop.getPilgrimageSpotId()
                                                : stop.getNearbyAttractionId(),
                                        stop.getName(),
                                        stop.getArrivalTime(),
                                        stop.getStayMinutes(),
                                        stop.getReason()))
                                .toList()))
                .toList();
        return new TripResponse(plan.getId(), plan.getContent().getId(), plan.getTitle(), days, plan.getShareToken());
    }

    @Transactional
    public TripResponse regenerate(Long userId, Long tripId, TripGenerateRequest request) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        requireAccess(plan, userId);
        AiRequestStatus aiStatus = layoutRoute(plan, request);
        recordAiRequest(plan, AiRequestType.TRIP_REGENERATE, aiStatus, userId);
        return toResponse(plan);
    }

    public TripSummaryResponse save(Long userId, Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        requireAccess(plan, userId);
        if (plan.isUnowned()) {
            plan.assignOwner(userRef(userId)); // 비회원 생성 → 로그인 후 저장 플로우의 소유권 클레임
        }
        plan.markSaved();
        tripPlanRepository.save(plan);
        return toSummary(plan);
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> findMyTrips(Long userId) {
        return tripPlanRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private TripSummaryResponse toSummary(TripPlan plan) {
        return new TripSummaryResponse(plan.getId(), plan.getTitle(), plan.getDurationDays(), plan.getStatus().name());
    }

    @Transactional(readOnly = true)
    public TripResponse findTrip(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return toResponse(plan);
    }

    @Transactional
    public void delete(Long userId, Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        requireAccess(plan, userId);
        tripPlanRepository.delete(plan);
    }

    @Transactional
    public TripShareResponse share(Long userId, Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        requireAccess(plan, userId);
        if (plan.isUnowned()) {
            plan.assignOwner(userRef(userId)); // 로그인 후 이어서 공유 플로우
        }
        plan.assignShareToken(UUID.randomUUID().toString().replace("-", ""));
        String shareUrl = "https://seongjiduk.example/share/" + plan.getShareToken();
        return new TripShareResponse(plan.getId(), shareUrl, plan.getTitle() + " 공유");
    }
}

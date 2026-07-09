package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.spot.dto.request.SpotReportCreateRequest;
import com.sungjiduk.backend.spot.dto.response.NearbyAttractionsResponse;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
import com.sungjiduk.backend.spot.dto.response.SpotReportResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.NearbyAttractionsProvider;
import com.sungjiduk.backend.spot.infra.StreetViewClient;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;

@Service
@Transactional(readOnly = true)
public class SpotService {

    private final PilgrimageSpotRepository spotRepository;
    private final NearbyAttractionsProvider attractionsProvider;
    private final StreetViewClient streetViewClient;
    // RedisTemplate 주입
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration SPOT_CACHE_TTL = Duration.ofHours(6);

    // Redis keys
    private String streetViewKey(Long spotId) {
        return "street-view:spot:" + spotId;
    }

    private String nearbyAttractionsKey(Long spotId) {
        return "nearby-attractions:spot:" + spotId;
    }

    private String nearbyRestaurantsKey(Long spotId) {
        return "nearby-restaurants:spot:" + spotId;
    }

    private String nearbyThemeKey(Long spotId, String theme) {
        return "nearby-theme:spot:" + spotId + ":theme:" + theme;
    }

    public SpotService(PilgrimageSpotRepository spotRepository, NearbyAttractionsProvider attractionsProvider,
                       StreetViewClient streetViewClient, RedisTemplate<String, Object> redisTemplate) {
        this.spotRepository = spotRepository;
        this.attractionsProvider = attractionsProvider;
        this.streetViewClient = streetViewClient;
        this.redisTemplate = redisTemplate;
    }


    /** 여행 미리보기용 실거리뷰(장면컷 옆 실사) — 키는 서버에만 있고 이미지 바이트를 프록시한다. */
    public java.util.Optional<byte[]> streetView(Long spotId) {
        Object cached = redisTemplate.opsForValue().get(streetViewKey(spotId));
        if (cached instanceof byte[] bytes) {
            return java.util.Optional.of(bytes);
        }

        var spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new com.sungjiduk.backend.common.exception.BusinessException(
                com.sungjiduk.backend.common.constants.ErrorCode.SPOT_NOT_FOUND));

        var image = streetViewClient.fetchImage(
            spot.getLat().doubleValue(),
            spot.getLng().doubleValue()
        );

        image.ifPresent(bytes ->
            redisTemplate.opsForValue().set(streetViewKey(spotId), bytes, SPOT_CACHE_TTL)
        );

        return image;
    }

    /** 테마 선택 레이어 — 테마 → (Places 타입, 기본 카테고리). 볼거리·먹을거리는 기존 전용 경로 재사용. */
    private static final java.util.Map<String, String[]> THEME_TYPES = java.util.Map.of(
            "CAFE", new String[]{"cafe", "카페"},
            "SHOPPING", new String[]{"shopping_mall", "쇼핑"},
            "LODGING", new String[]{"lodging", "숙소"});


    private static final int MAX_ATTRACTIONS = 6;

    /**
     * 테마 선택 주변 장소 — 일정 확인 후 단계에서 사용자가 고른 테마만 얹는다.
     * SIGHTS/FOOD는 기존 전용 조회(정렬·캐시 포함)로 위임, 나머지는 별점순.
     */
    public NearbyAttractionsResponse findNearbyByTheme(Long spotId, String theme) {
        String key = theme == null ? "" : theme.toUpperCase(java.util.Locale.ROOT);
        if ("SIGHTS".equals(key)) {
            return findNearbyAttractions(spotId);
        }
        if ("FOOD".equals(key)) {
            return findNearbyRestaurants(spotId);
        }
        String[] mapping = THEME_TYPES.get(key);
        if (mapping == null) {
            throw new com.sungjiduk.backend.common.exception.BusinessException(
                    com.sungjiduk.backend.common.constants.ErrorCode.VALIDATION_FAILED);
        }
        // key값
        String cacheKey = nearbyThemeKey(spotId, key);
        // 읽기
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof NearbyAttractionsResponse response) {
            return response;
        }
        PilgrimageSpot spot = spotRepository.findByIdOrThrow(spotId);
        var places = attractionsProvider
                .findNearbyByType(spot.getLat().doubleValue(), spot.getLng().doubleValue(), mapping[0], mapping[1])
                .stream()
                .filter(a -> a.rating() != null && a.ratingCount() != null)
                .sorted(java.util.Comparator
                        .comparing(NearbyAttractionsProvider.Attraction::rating)
                        .thenComparing(NearbyAttractionsProvider.Attraction::ratingCount)
                        .reversed())
                .limit(MAX_ATTRACTIONS)
                .map(a -> new NearbyAttractionsResponse.AttractionSummary(
                        a.name(), a.category(), a.rating(), a.ratingCount(), a.lat(), a.lng(), a.mapsUrl()))
                .toList();
        NearbyAttractionsResponse response = new NearbyAttractionsResponse(spotId, places);
        // 쓰기
        if (!places.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, response, SPOT_CACHE_TTL);
        }
        return response;
    }

    /** 주변 맛집 — 별점 높은 순(동점은 리뷰수), 여정 ⑤ 레이어. */
    public NearbyAttractionsResponse findNearbyRestaurants(Long spotId) {
        // 읽기
        Object cached = redisTemplate.opsForValue().get(nearbyRestaurantsKey(spotId));
        if (cached instanceof NearbyAttractionsResponse response) {
            return response;
        }
        PilgrimageSpot spot = spotRepository.findByIdOrThrow(spotId);
        var restaurants = attractionsProvider
                .findNearbyRestaurants(spot.getLat().doubleValue(), spot.getLng().doubleValue())
                .stream()
                .filter(a -> a.rating() != null && a.ratingCount() != null)
                .sorted(java.util.Comparator
                        .comparing(NearbyAttractionsProvider.Attraction::rating)
                        .thenComparing(NearbyAttractionsProvider.Attraction::ratingCount)
                        .reversed())
                .limit(MAX_ATTRACTIONS)
                .map(a -> new NearbyAttractionsResponse.AttractionSummary(
                        a.name(), a.category(), a.rating(), a.ratingCount(), a.lat(), a.lng(), a.mapsUrl()))
                .toList();
        NearbyAttractionsResponse response = new NearbyAttractionsResponse(spotId, restaurants);
        // 쓰기
        if (!restaurants.isEmpty()) {
            redisTemplate.opsForValue().set(nearbyRestaurantsKey(spotId), response, SPOT_CACHE_TTL);
        }
        return response;
    }

    public NearbyAttractionsResponse findNearbyAttractions(Long spotId) {
        Object cached = redisTemplate.opsForValue().get(nearbyAttractionsKey(spotId));
        if (cached instanceof NearbyAttractionsResponse response) {
            return response;
        }
        PilgrimageSpot spot = spotRepository.findByIdOrThrow(spotId);
        var attractions = attractionsProvider
                .findNearby(spot.getLat().doubleValue(), spot.getLng().doubleValue())
                .stream()
                .filter(a -> a.rating() != null && a.ratingCount() != null)
                .sorted(java.util.Comparator.comparing(
                        NearbyAttractionsProvider.Attraction::ratingCount).reversed())
                .limit(MAX_ATTRACTIONS)
                .map(a -> new NearbyAttractionsResponse.AttractionSummary(
                        a.name(), a.category(), a.rating(), a.ratingCount(), a.lat(), a.lng(), a.mapsUrl()))
                .toList();
        NearbyAttractionsResponse response = new NearbyAttractionsResponse(spotId, attractions);
        // 쓰기부분 redis로 변경, 빈 결과는 캐시하지 않음.
        if (!attractions.isEmpty()) {
            redisTemplate.opsForValue().set(nearbyAttractionsKey(spotId), response, SPOT_CACHE_TTL);
        }
        return response;
    }

    public SpotDetailResponse findSpot(Long spotId) {
        PilgrimageSpot spot = spotRepository.findByIdOrThrow(spotId);
        return new SpotDetailResponse(
                spot.getId(),
                spot.getName(),
                spot.getCity(),
                spot.getAddress(),
                spot.getLat().doubleValue(),
                spot.getLng().doubleValue(),
                spot.getRecommendedDurationMin(),
                spot.getReferenceUrl());
    }

    // TODO: SpotReport 엔티티/테이블 확정 후 실제 저장. 현재는 접수 응답만 반환한다.
    public SpotReportResponse createReport(SpotReportCreateRequest request) {
        return new SpotReportResponse(1L, "PENDING");
    }
}

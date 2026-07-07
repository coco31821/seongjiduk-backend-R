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

@Service
@Transactional(readOnly = true)
public class SpotService {

    private final PilgrimageSpotRepository spotRepository;
    private final NearbyAttractionsProvider attractionsProvider;
    private final StreetViewClient streetViewClient;

    public SpotService(PilgrimageSpotRepository spotRepository, NearbyAttractionsProvider attractionsProvider,
                       StreetViewClient streetViewClient) {
        this.spotRepository = spotRepository;
        this.attractionsProvider = attractionsProvider;
        this.streetViewClient = streetViewClient;
    }

    /** 성지별 스트리트뷰 이미지 캐시 — 빈 결과는 캐시하지 않는다(키 추가 시 재시도). */
    private final java.util.Map<Long, byte[]> streetViewCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** 여행 미리보기용 실거리뷰(장면컷 옆 실사) — 키는 서버에만 있고 이미지 바이트를 프록시한다. */
    public java.util.Optional<byte[]> streetView(Long spotId) {
        byte[] cached = streetViewCache.get(spotId);
        if (cached != null) {
            return java.util.Optional.of(cached);
        }
        var spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new com.sungjiduk.backend.common.exception.BusinessException(
                        com.sungjiduk.backend.common.constants.ErrorCode.SPOT_NOT_FOUND));
        var image = streetViewClient.fetchImage(spot.getLat().doubleValue(), spot.getLng().doubleValue());
        image.ifPresent(bytes -> streetViewCache.put(spotId, bytes));
        return image;
    }

    /** 성지별 주변 관광지 캐시(외부 API 절약). 빈 결과는 캐시하지 않아 키 추가 시 재시도된다. */
    private final java.util.Map<Long, NearbyAttractionsResponse> nearbyCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** 성지별 주변 맛집 캐시 — 관광지와 정렬 기준이 달라(별점순) 분리 보관. */
    private final java.util.Map<Long, NearbyAttractionsResponse> restaurantCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final int MAX_ATTRACTIONS = 6;

    /** 주변 맛집 — 별점 높은 순(동점은 리뷰수), 여정 ⑤ 레이어. */
    public NearbyAttractionsResponse findNearbyRestaurants(Long spotId) {
        NearbyAttractionsResponse cached = restaurantCache.get(spotId);
        if (cached != null) {
            return cached;
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
        if (!restaurants.isEmpty()) {
            restaurantCache.put(spotId, response);
        }
        return response;
    }

    public NearbyAttractionsResponse findNearbyAttractions(Long spotId) {
        NearbyAttractionsResponse cached = nearbyCache.get(spotId);
        if (cached != null) {
            return cached;
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
        if (!attractions.isEmpty()) {
            nearbyCache.put(spotId, response);
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

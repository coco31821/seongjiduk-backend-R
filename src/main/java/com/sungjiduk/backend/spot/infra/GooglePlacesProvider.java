package com.sungjiduk.backend.spot.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Google <b>Places API (New)</b> searchNearby 기반 주변 관광 명소.
 * (레거시 nearbysearch는 신규 프로젝트에서 차단 — REQUEST_DENIED 실측)
 * env {@code GOOGLE_MAPS_API_KEY}(Places API (New) 활성화 + billing)가 없거나 실패하면 빈 리스트.
 *
 * <p>공유 키 보호: 호출 전 {@link RateLimiter}로 분당 한도를 확인해 429/과금을 막는다(초과 시 빈 리스트).
 */
@Component
public class GooglePlacesProvider implements NearbyAttractionsProvider {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesProvider.class);
    private static final String RATE_KEY = "google:places";
    private static final int RADIUS_METERS = 900;
    private static final String FIELD_MASK =
            "places.displayName,places.rating,places.userRatingCount,places.location,"
                    + "places.googleMapsUri,places.primaryTypeDisplayName";

    private final String apiKey;
    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final RedisGuardProperties guardProps;

    public GooglePlacesProvider(
            @Value("${seongjiduk.geocoding.google.api-key:}") String apiKey,
            RateLimiter rateLimiter,
            RedisGuardProperties guardProps
    ) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://places.googleapis.com")
                .build();
        this.rateLimiter = rateLimiter;
        this.guardProps = guardProps;
    }

    @Override
    public List<Attraction> findNearby(double lat, double lng) {
        return search(lat, lng, "tourist_attraction", "명소");
    }

    @Override
    public List<Attraction> findNearbyRestaurants(double lat, double lng) {
        return search(lat, lng, "restaurant", "식당");
    }

    @Override
    public List<Attraction> findNearbyByType(double lat, double lng, String placeType, String defaultCategory) {
        return search(lat, lng, placeType, defaultCategory);
    }

    private List<Attraction> search(double lat, double lng, String includedType, String defaultCategory) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        if (!rateLimiter.tryAcquire(RATE_KEY, guardProps.getExternalRpm(), Duration.ofMinutes(1))) {
            log.warn("구글 Places API 분당 한도 초과 — 이번 호출 스킵(type={})", includedType);
            return List.of();
        }
        try {
            SearchNearbyResponse response = restClient.post()
                    .uri("/v1/places:searchNearby")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(Map.of(
                            "includedTypes", List.of(includedType),
                            "maxResultCount", 20,
                            "languageCode", "ko",
                            "locationRestriction", Map.of("circle", Map.of(
                                    "center", Map.of("latitude", lat, "longitude", lng),
                                    "radius", RADIUS_METERS))))
                    .retrieve()
                    .body(SearchNearbyResponse.class);
            if (response == null || response.places() == null) {
                return List.of();
            }
            return response.places().stream()
                    .filter(p -> p.displayName() != null && p.location() != null)
                    .map(p -> new Attraction(
                            p.displayName().text(),
                            p.primaryTypeDisplayName() == null ? defaultCategory : p.primaryTypeDisplayName().text(),
                            p.rating(),
                            p.userRatingCount(),
                            p.location().latitude(),
                            p.location().longitude(),
                            p.googleMapsUri()))
                    .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchNearbyResponse(List<Place> places) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Place(
            LocalizedText displayName,
            LocalizedText primaryTypeDisplayName,
            Double rating,
            Integer userRatingCount,
            LatLng location,
            String googleMapsUri
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocalizedText(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatLng(double latitude, double longitude) {
    }
}

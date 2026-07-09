package com.sungjiduk.backend.spot.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Google Geocoding API 기반 역지오코딩.
 * env {@code GOOGLE_MAPS_API_KEY}가 있으면 호출하고, 없거나 실패하면 {@link Optional#empty()}를 반환한다.
 * (주소는 루트 계산엔 불필요하지만 AI 설명·Day 라벨 품질에 도움 → 옵션.)
 *
 * <p>공유 키 보호: 호출 전 {@link RateLimiter}로 분당 한도를 확인해 429/과금을 막는다(초과 시 empty).
 */
@Component
public class GoogleReverseGeocoder implements ReverseGeocoder {

    private static final Logger log = LoggerFactory.getLogger(GoogleReverseGeocoder.class);
    private static final String RATE_KEY = "google:geocoding";

    private final String apiKey;
    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final RedisGuardProperties guardProps;

    public GoogleReverseGeocoder(
            @Value("${seongjiduk.geocoding.google.api-key:}") String apiKey,
            RateLimiter rateLimiter,
            RedisGuardProperties guardProps
    ) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://maps.googleapis.com")
                .build();
        this.rateLimiter = rateLimiter;
        this.guardProps = guardProps;
    }

    private boolean rateLimited(String op) {
        if (!rateLimiter.tryAcquire(RATE_KEY, guardProps.getExternalRpm(), Duration.ofMinutes(1))) {
            log.warn("구글 Geocoding 분당 한도 초과 — 이번 호출 스킵({})", op);
            return true;
        }
        return false;
    }

    @Override
    public Optional<GeoResult> reverse(double lat, double lng) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        if (rateLimited("reverse")) {
            return Optional.empty();
        }
        try {
            GeocodeResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/geocode/json")
                            .queryParam("latlng", lat + "," + lng)
                            .queryParam("language", "ja")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(GeocodeResponse.class);

            if (response == null || !"OK".equals(response.status()) || response.results().isEmpty()) {
                return Optional.empty();
            }
            GeocodeResult top = response.results().get(0);
            return Optional.of(new GeoResult(top.formattedAddress(), extractCity(top)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LatLng> forward(String address) {
        if (apiKey == null || apiKey.isBlank() || address == null || address.isBlank()) {
            return Optional.empty();
        }
        if (rateLimited("forward")) {
            return Optional.empty();
        }
        try {
            GeocodeResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/geocode/json")
                            .queryParam("address", address)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(GeocodeResponse.class);
            if (response == null || !"OK".equals(response.status()) || response.results().isEmpty()) {
                return Optional.empty();
            }
            Geometry geometry = response.results().get(0).geometry();
            if (geometry == null || geometry.location() == null) {
                return Optional.empty();
            }
            return Optional.of(new LatLng(geometry.location().lat(), geometry.location().lng()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String extractCity(GeocodeResult result) {
        return result.addressComponents().stream()
                .filter(c -> c.types().contains("locality"))
                .map(AddressComponent::longName)
                .findFirst()
                .orElseGet(() -> result.addressComponents().stream()
                        .filter(c -> c.types().contains("administrative_area_level_1"))
                        .map(AddressComponent::longName)
                        .findFirst()
                        .orElse(null));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodeResponse(String status, List<GeocodeResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodeResult(
            @com.fasterxml.jackson.annotation.JsonProperty("formatted_address") String formattedAddress,
            @com.fasterxml.jackson.annotation.JsonProperty("address_components") List<AddressComponent> addressComponents,
            Geometry geometry
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AddressComponent(
            @com.fasterxml.jackson.annotation.JsonProperty("long_name") String longName,
            List<String> types
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Geometry(Location location) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Location(double lat, double lng) {
    }
}

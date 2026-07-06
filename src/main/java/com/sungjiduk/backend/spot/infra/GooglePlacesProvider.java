package com.sungjiduk.backend.spot.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Places Nearby Search 기반 주변 관광 명소.
 * env {@code GOOGLE_MAPS_API_KEY}(Places API 활성화 필요)가 없거나 실패하면 빈 리스트.
 */
@Component
public class GooglePlacesProvider implements NearbyAttractionsProvider {

    private static final int RADIUS_METERS = 900;
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "tourist_attraction", "명소",
            "shrine", "신사",
            "temple", "사찰",
            "park", "공원",
            "museum", "박물관",
            "shopping_mall", "쇼핑",
            "amusement_park", "놀이공원"
    );

    private final String apiKey;
    private final RestClient restClient;

    public GooglePlacesProvider(@Value("${seongjiduk.geocoding.google.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://maps.googleapis.com")
                .build();
    }

    @Override
    public List<Attraction> findNearby(double lat, double lng) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        try {
            PlacesResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/place/nearbysearch/json")
                            .queryParam("location", lat + "," + lng)
                            .queryParam("radius", RADIUS_METERS)
                            .queryParam("type", "tourist_attraction")
                            .queryParam("language", "ko")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(PlacesResponse.class);
            if (response == null || response.results() == null || !"OK".equals(response.status())) {
                return List.of();
            }
            return response.results().stream()
                    .filter(r -> r.name() != null && r.geometry() != null)
                    .map(r -> new Attraction(
                            r.name(),
                            label(r.types()),
                            r.rating(),
                            r.userRatingsTotal(),
                            r.geometry().location().lat(),
                            r.geometry().location().lng(),
                            "https://www.google.com/maps/place/?q=place_id:" + r.placeId()))
                    .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private String label(List<String> types) {
        if (types == null) {
            return "명소";
        }
        return types.stream()
                .map(TYPE_LABELS::get)
                .filter(l -> l != null)
                .findFirst()
                .orElse("명소");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlacesResponse(String status, List<PlaceResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlaceResult(
            String name,
            Double rating,
            @JsonProperty("user_ratings_total") Integer userRatingsTotal,
            @JsonProperty("place_id") String placeId,
            List<String> types,
            Geometry geometry
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Geometry(LatLng location) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatLng(double lat, double lng) {
    }
}

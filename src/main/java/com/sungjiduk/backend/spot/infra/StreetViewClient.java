package com.sungjiduk.backend.spot.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Google Street View Static 프록시용 클라이언트.
 * 키를 서버에 유지하기 위해 이미지를 백엔드가 대신 받아온다(프론트에 키 노출 금지).
 * 메타데이터 조회는 무과금 — imagery 없는 좌표는 이미지 요청 전에 걸러 비용을 아낀다.
 */
@Component
public class StreetViewClient {

    private static final String SIZE = "640x400";

    private final RestClient restClient = RestClient.builder().build();
    private final String apiKey;
    private final String metadataUrl;
    private final String imageUrl;

    public StreetViewClient(@Value("${seongjiduk.geocoding.google.api-key:}") String apiKey,
                            @Value("${seongjiduk.geocoding.google.maps-base-url:https://maps.googleapis.com}") String mapsBaseUrl) {
        this.apiKey = apiKey;
        this.metadataUrl = mapsBaseUrl + "/maps/api/streetview/metadata";
        this.imageUrl = mapsBaseUrl + "/maps/api/streetview";
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 실외 파노라마가 있는 좌표만 이미지 바이트를 반환. 없거나 실패면 empty. */
    public Optional<byte[]> fetchImage(double lat, double lng) {
        if (!enabled()) {
            return Optional.empty();
        }
        try {
            Metadata metadata = restClient.get()
                    .uri(metadataUrl + "?location={loc}&source=outdoor&key={key}",
                            lat + "," + lng, apiKey)
                    .retrieve()
                    .body(Metadata.class);
            if (metadata == null || !"OK".equals(metadata.status())) {
                return Optional.empty();
            }
            byte[] image = restClient.get()
                    .uri(imageUrl + "?size={size}&location={loc}&source=outdoor&key={key}",
                            SIZE, lat + "," + lng, apiKey)
                    .retrieve()
                    .body(byte[].class);
            return image == null || image.length == 0 ? Optional.empty() : Optional.of(image);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record Metadata(String status) {
    }
}

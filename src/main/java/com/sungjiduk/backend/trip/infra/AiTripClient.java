package com.sungjiduk.backend.trip.infra;

import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.trip.infra.dto.AiTripRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * ai-service(LangGraph) 일정 생성 호출. 실패는 호출부(TripService)가 로컬 폴백으로 처리하므로
 * 여기서는 예외를 그대로 전파한다.
 */
@Component
public class AiTripClient {

    private final RestClient restClient;

    public AiTripClient(@Value("${seongjiduk.ai-service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public AiTripLayout generate(AiTripRequest request) {
        return restClient.post()
                .uri("/ai/trips/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiTripLayout.class);
    }
}

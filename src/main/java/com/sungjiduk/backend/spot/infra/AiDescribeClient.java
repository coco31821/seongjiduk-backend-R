package com.sungjiduk.backend.spot.infra;

import com.sungjiduk.backend.spot.infra.dto.AiDescribeRequest;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * ai-service(LangGraph) 성지 장면 설명 호출. 실패는 호출부(ContentService)가
 * "설명 없음" 폴백으로 처리하므로 여기서는 예외를 그대로 전파한다.
 */
@Component
public class AiDescribeClient {

    private final RestClient restClient;

    public AiDescribeClient(@Value("${seongjiduk.ai-service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public AiDescribeResult describe(AiDescribeRequest request) {
        return restClient.post()
                .uri("/ai/spots/describe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiDescribeResult.class);
    }
}

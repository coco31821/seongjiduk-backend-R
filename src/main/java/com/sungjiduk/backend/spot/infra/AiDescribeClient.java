package com.sungjiduk.backend.spot.infra;

import com.sungjiduk.backend.spot.infra.dto.AiDescribeRequest;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult;
import com.sungjiduk.backend.common.config.AiRestClientFactory;
import com.sungjiduk.backend.common.ratelimit.AiConcurrencyGuard;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * ai-service(LangGraph) 성지 장면 설명 호출. 실패는 호출부(ContentService)가
 * "설명 없음" 폴백으로 처리하므로 여기서는 예외를 그대로 전파한다.
 * HTTP/1.1 강제: JDK HttpClient의 h2c 업그레이드를 uvicorn(h11)이 거부하며 body가 유실됨.
 */
@Component
public class AiDescribeClient {

    private final RestClient restClient;

    private final AiConcurrencyGuard guard;
    public AiDescribeClient(AiRestClientFactory factory, AiConcurrencyGuard guard) {
        this.restClient = factory.create(Duration.ofSeconds(90));
        this.guard = guard;
    }

    public AiDescribeResult describe(AiDescribeRequest request) {
        return guard.execute("describe", () -> restClient.post()
                .uri("/ai/spots/describe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiDescribeResult.class));
    }
}

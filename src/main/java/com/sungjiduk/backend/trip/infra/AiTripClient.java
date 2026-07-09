package com.sungjiduk.backend.trip.infra;

import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter;
import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter.Permit;
import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.trip.infra.dto.AiTripRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * ai-service(LangGraph) 일정 생성 호출. 실패는 호출부(TripService)가 로컬 폴백으로 처리하므로
 * 여기서는 예외를 그대로 전파한다.
 * HTTP/1.1 강제: JDK HttpClient의 h2c 업그레이드를 uvicorn(h11)이 거부하며 body가 유실됨(실측).
 *
 * <p>처리량 가드: 인스턴스를 늘릴 수 없으므로 {@link ConcurrencyLimiter}로 동시 호출 수를 묶는다.
 * 자리가 없으면 짧게 큐잉 후 {@link AiBusyException}으로 폴백을 유도해 ai-service 과부하를 막는다.
 */
@Component
public class AiTripClient {

    private static final String LIMIT_KEY = "ai:generate";

    private final RestClient restClient;
    private final ConcurrencyLimiter concurrencyLimiter;
    private final RedisGuardProperties guardProps;

    public AiTripClient(
            @Value("${seongjiduk.ai-service.base-url:http://localhost:8000}") String baseUrl,
            ConcurrencyLimiter concurrencyLimiter,
            RedisGuardProperties guardProps
    ) {
        HttpClient http1Client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(http1Client))
                .build();
        this.concurrencyLimiter = concurrencyLimiter;
        this.guardProps = guardProps;
    }

    public AiTripLayout generate(AiTripRequest request) {
        Optional<Permit> permit = concurrencyLimiter.acquire(
                LIMIT_KEY, guardProps.getAiMaxConcurrent(), Duration.ofMillis(guardProps.getAiWaitMs()));
        if (permit.isEmpty()) {
            throw new AiBusyException("AI 생성 동시성 한도 초과(대기 " + guardProps.getAiWaitMs() + "ms 초과)");
        }
        try (Permit p = permit.get()) {
            return restClient.post()
                    .uri("/ai/trips/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiTripLayout.class);
        }
    }
}

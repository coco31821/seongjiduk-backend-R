package com.sungjiduk.backend.trip.infra;

import com.sungjiduk.backend.common.config.AiRestClientFactory;
import com.sungjiduk.backend.common.ratelimit.AiConcurrencyGuard;
import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.trip.infra.dto.AiTripRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

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

    private final RestClient restClient;

    public AiTripClient(
            AiRestClientFactory factory,
            AiConcurrencyGuard guard
    ) {
        this.restClient = factory.create(Duration.ofSeconds(45));
        this.guard = guard;
    }
    private final AiConcurrencyGuard guard;

    public AiTripLayout generate(AiTripRequest request) {
        return guard.execute("trip", () -> restClient.post()
                    .uri("/ai/trips/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiTripLayout.class));
    }
}

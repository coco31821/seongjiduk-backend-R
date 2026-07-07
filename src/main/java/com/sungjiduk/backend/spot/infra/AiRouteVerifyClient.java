package com.sungjiduk.backend.spot.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

/**
 * ai-service 블로그 루트 추출·합의 호출. HTTP/1.1 강제(h2c→uvicorn body 유실 방지, 실측).
 * 실패는 호출부가 빈 결과로 처리.
 */
@Component
public class AiRouteVerifyClient {

    private final RestClient restClient;

    public AiRouteVerifyClient(@Value("${seongjiduk.ai-service.base-url:http://localhost:8000}") String baseUrl) {
        HttpClient http1Client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(http1Client))
                .build();
    }

    public VerifyResult verify(VerifyRequest request) {
        return restClient.post()
                .uri("/ai/routes/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VerifyResult.class);
    }

    public record VerifyRequest(Content content, List<RouteSpot> spots, List<BlogPost> posts) {
        public record Content(Long id, String title) {
        }

        public record RouteSpot(Long id, String name, String koreanName) {
        }

        public record BlogPost(String title, String text, String postdate, String link) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifyResult(
            String provider,
            int postCount,
            int usedPostCount,
            List<SpotMention> spotMentions,
            List<VerifiedPair> verifiedPairs,
            List<VerifiedCourse> courses
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record VerifiedCourse(int rank, List<Long> spotIds, int supportCount, List<Integer> postIndexes) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SpotMention(Long spotId, int count) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record VerifiedPair(Long fromSpotId, Long toSpotId, int count) {
        }
    }
}

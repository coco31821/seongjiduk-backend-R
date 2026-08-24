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

/**
 * 네이버 검색 API(블로그) — 공식 API만 사용(대량 크롤링 금지 원칙).
 * 키 없으면 빈 리스트 → 검증 기능이 조용히 비활성.
 *
 * <p>공유 키 보호: 호출 전 {@link RateLimiter}로 분당 한도를 확인해 429/과금을 막는다(초과 시 빈 리스트).
 */
@Component
public class NaverBlogClient {

    private static final Logger log = LoggerFactory.getLogger(NaverBlogClient.class);
    private static final String RATE_KEY = "naver:blog";

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final RedisGuardProperties guardProps;

    public NaverBlogClient(
            @Value("${seongjiduk.naver.client-id:}") String clientId,
            @Value("${seongjiduk.naver.client-secret:}") String clientSecret,
            @Value("${seongjiduk.naver.base-url:https://openapi.naver.com}") String baseUrl,
            RateLimiter rateLimiter,
            RedisGuardProperties guardProps
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.rateLimiter = rateLimiter;
        this.guardProps = guardProps;
    }

    public boolean enabled() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /** 블로그 검색 — 제목·링크만 사용(설명은 짧아 순서 추출에 부적합). */
    public List<BlogItem> search(String query, int display) {
        if (!enabled()) {
            return List.of();
        }
        if (!rateLimiter.tryAcquire(RATE_KEY, guardProps.getExternalRpm(), Duration.ofMinutes(1))) {
            log.warn("네이버 블로그 API 분당 한도 초과 — 이번 호출 스킵(query={})", query);
            return List.of();
        }
        try {
            SearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/blog.json")
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("sort", "sim")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(SearchResponse.class);
            if (response == null || response.items() == null) {
                return List.of();
            }
            return response.items().stream()
                    .map(item -> new BlogItem(stripTags(item.title()), item.link(), item.postdate()))
                    .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private String stripTags(String s) {
        return s == null ? "" : s.replaceAll("<[^>]+>", "");
    }

    public record BlogItem(String title, String link, String postdate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Item(String title, String link, String postdate) {
    }
}

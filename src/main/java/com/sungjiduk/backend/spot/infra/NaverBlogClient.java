package com.sungjiduk.backend.spot.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 네이버 검색 API(블로그) — 공식 API만 사용(대량 크롤링 금지 원칙).
 * 키 없으면 빈 리스트 → 검증 기능이 조용히 비활성.
 */
@Component
public class NaverBlogClient {

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public NaverBlogClient(
            @Value("${seongjiduk.naver.client-id:}") String clientId,
            @Value("${seongjiduk.naver.client-secret:}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.builder().baseUrl("https://openapi.naver.com").build();
    }

    public boolean enabled() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /** 블로그 검색 — 제목·링크만 사용(설명은 짧아 순서 추출에 부적합). */
    public List<BlogItem> search(String query, int display) {
        if (!enabled()) {
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

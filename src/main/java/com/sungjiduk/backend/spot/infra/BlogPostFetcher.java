package com.sungjiduk.backend.spot.infra;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * 검색 API가 준 링크의 본문 텍스트를 1회 조회해 구조 추출용으로만 사용한다.
 * 원문은 저장하지 않는다(저작권 원칙 — ADR-0002 계열). 실패는 조용히 스킵.
 */
@Component
public class BlogPostFetcher {

    private static final int MAX_CHARS = 4000;

    private final RestClient restClient = RestClient.builder().build();

    public Optional<String> fetchText(String link) {
        try {
            String url = toMobileUrl(link);
            String html = restClient.get().uri(url)
                    .header("User-Agent", "Mozilla/5.0 (seongjiduk route-verify; contact: admin)")
                    .retrieve()
                    .body(String.class);
            if (html == null) {
                return Optional.empty();
            }
            return extractText(html);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * HTML에서 본문 텍스트를 추출한다. 본문이 빈약하면(스크립트 렌더링 페이지 —
     * 티스토리 스킨 등) og:description 메타로 폴백한다: 짧아도 언급 집계에는 유효하다.
     */
    static Optional<String> extractText(String html) {
        String text = html
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;|&amp;|&lt;|&gt;|&quot;|&#\\d+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() >= 300) {
            return Optional.of(text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text);
        }
        String description = metaContent(html, "og:description");
        if (description != null && description.length() >= 50) {
            return Optional.of(description);
        }
        return Optional.empty();
    }

    /** {@code <meta property="..." content="...">} 값 추출 (속성 순서 양방향 지원). */
    private static String metaContent(String html, String property) {
        var patterns = java.util.List.of(
                java.util.regex.Pattern.compile(
                        "<meta[^>]+property=[\"']" + java.util.regex.Pattern.quote(property)
                                + "[\"'][^>]+content=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile(
                        "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']"
                                + java.util.regex.Pattern.quote(property) + "[\"']", java.util.regex.Pattern.CASE_INSENSITIVE));
        for (var pattern : patterns) {
            var matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    /** blog.naver.com은 iframe 셸이라 본문이 비어 있다 → 모바일 뷰로 변환해 실본문 조회. */
    private String toMobileUrl(String link) {
        if (link == null) {
            return "";
        }
        return link.replaceFirst("^https?://blog\\.naver\\.com/", "https://m.blog.naver.com/");
    }
}

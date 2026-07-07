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
            String text = html
                    .replaceAll("(?is)<script.*?</script>", " ")
                    .replaceAll("(?is)<style.*?</style>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;|&amp;|&lt;|&gt;|&quot;|&#\\d+;", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (text.length() < 300) {
                return Optional.empty(); // 본문이 없다시피 한 페이지(스크립트 렌더링 등)는 제외
            }
            return Optional.of(text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** blog.naver.com은 iframe 셸이라 본문이 비어 있다 → 모바일 뷰로 변환해 실본문 조회. */
    private String toMobileUrl(String link) {
        if (link == null) {
            return "";
        }
        return link.replaceFirst("^https?://blog\\.naver\\.com/", "https://m.blog.naver.com/");
    }
}

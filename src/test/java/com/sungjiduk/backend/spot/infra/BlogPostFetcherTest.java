package com.sungjiduk.backend.spot.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlogPostFetcher")
class BlogPostFetcherTest {

    @Nested
    @DisplayName("extractText는")
    class ExtractText {

        @Test
        @DisplayName("본문이 충분하면 태그를 벗겨 텍스트를 반환한다")
        void stripsTagsWhenBodyIsLongEnough() {
            // given
            String html = "<html><body><p>" + "칸다묘진 방문 후기 ".repeat(40) + "</p></body></html>";

            // when
            Optional<String> text = BlogPostFetcher.extractText(html);

            // then
            assertThat(text).isPresent();
            assertThat(text.get()).contains("칸다묘진 방문 후기");
            assertThat(text.get()).doesNotContain("<p>");
        }

        @Test
        @DisplayName("본문이 짧아도 og:description 메타가 충분하면 그걸로 폴백한다")
        void fallsBackToOgDescription() {
            // given — 스크립트 렌더링 페이지(본문 비어 있음) + 메타 설명
            String description = "러브라이브 성지순례로 칸다묘진과 아키하바라, 타케무라를 다녀온 후기입니다. "
                    + "칸다묘진에서 에마를 보고 아키하바라 거리를 걸었어요.";
            String html = "<html><head><meta property=\"og:description\" content=\"" + description + "\"/>"
                    + "</head><body><div id=\"app\"></div><script>render()</script></body></html>";

            // when
            Optional<String> text = BlogPostFetcher.extractText(html);

            // then
            assertThat(text).isPresent();
            assertThat(text.get()).contains("칸다묘진과 아키하바라");
        }

        @Test
        @DisplayName("본문도 메타도 빈약하면 빈 결과를 반환한다")
        void emptyWhenNothingUseful() {
            // given
            String html = "<html><head><meta property=\"og:description\" content=\"짧음\"/></head>"
                    + "<body><script>spa()</script></body></html>";

            // when
            Optional<String> text = BlogPostFetcher.extractText(html);

            // then
            assertThat(text).isEmpty();
        }
    }
}

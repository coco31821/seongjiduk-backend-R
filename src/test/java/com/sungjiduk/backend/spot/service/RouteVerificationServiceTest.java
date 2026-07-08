package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.AiRouteVerifyClient;
import com.sungjiduk.backend.spot.infra.AiRouteVerifyClient.VerifyResult;
import com.sungjiduk.backend.spot.infra.BlogPostFetcher;
import com.sungjiduk.backend.spot.infra.NaverBlogClient;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@SpringBootTest
@Transactional
@DisplayName("RouteVerificationService")
class RouteVerificationServiceTest {

    @Autowired
    private RouteVerificationService routeVerificationService;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    // 외부(네이버·블로그·ai)는 전부 mock
    @MockitoBean
    private NaverBlogClient naverBlogClient;

    @MockitoBean
    private BlogPostFetcher postFetcher;

    @MockitoBean
    private AiRouteVerifyClient aiRouteVerifyClient;

    @MockitoBean
    private Clock clock;

    private void nowAt(Instant instant) {
        given(clock.instant()).willReturn(instant);
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
    }

    private Content savedContentWithSpot() {
        Content content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "설명"));
        spotRepository.save(PilgrimageSpot.create(
                content, "神田明神", "東京都", new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                "千代田区", 40, null));
        return content;
    }

    @Nested
    @DisplayName("verify는")
    class Verify {

        @Test
        @DisplayName("네이버 키가 없으면 available=false 빈 결과를 반환한다")
        void unavailableWithoutKey() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(false);

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then
            assertThat(response.available()).isFalse();
            assertThat(response.verifiedPairs()).isEmpty();
        }

        @Test
        @DisplayName("검색→본문→ai 합의 결과를 매핑해 반환한다")
        void mapsConsensusResult() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("후기1", "https://blog.naver.com/a/1", "20260701"),
                    new NaverBlogClient.BlogItem("후기2", "https://blog.naver.com/b/2", "20260702")
            ));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 2, 2,
                    List.of(new VerifyResult.SpotMention(1L, 2, List.of())),
                    List.of(new VerifyResult.VerifiedPair(1L, 2L, 2)), List.of(), List.of()));

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then
            assertThat(response.available()).isTrue();
            assertThat(response.usedPostCount()).isEqualTo(2);
            assertThat(response.verifiedPairs()).hasSize(1);
            assertThat(response.verifiedPairs().get(0).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("코스의 postIndexes를 출처(제목·링크·날짜)로 매핑한다")
        void mapsCourseSources() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("최신후기", "https://blog.naver.com/new/1", "20260705"),
                    new NaverBlogClient.BlogItem("옛후기", "https://blog.naver.com/old/2", "20250101")
            ));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 2, 2, List.of(), List.of(),
                    List.of(new VerifyResult.VerifiedCourse(1, List.of(5L, 6L), 2, List.of(0, 1))), List.of()));

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then — 최신순 정렬로 index0=최신후기
            assertThat(response.courses()).hasSize(1);
            var course = response.courses().get(0);
            assertThat(course.spotIds()).containsExactly(5L, 6L);
            assertThat(course.sources()).extracting(RouteVerificationResponse.Source::title)
                    .containsExactly("최신후기", "옛후기");
        }

        @Test
        @DisplayName("같은 작품 재조회 시 검색·ai를 다시 호출하지 않는다(캐시)")
        void cachesPerContent() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("후기1", "https://blog.naver.com/a/1", "20260701")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 1, 1, List.of(), List.of(new VerifyResult.VerifiedPair(1L, 2L, 1)), List.of(), List.of()));

            // when
            routeVerificationService.verify(content.getId());
            routeVerificationService.verify(content.getId());

            // then — 검색은 쿼리 수(2)만큼만, 재조회에서 추가 호출 없음
            then(naverBlogClient).should(times(2)).search(anyString(), anyInt());
            then(aiRouteVerifyClient).should(times(1)).verify(any());
        }

        @Test
        @DisplayName("cachedCourseSpotIds는 캐시된 검증 코스의 스팟 순서를 외부 호출 없이 돌려준다")
        void cachedCourseSpotIdsReadsCacheOnly() {
            // given — 캐시 전: 빈 결과, 외부 호출 없음
            Content content = savedContentWithSpot();
            assertThat(routeVerificationService.cachedCourseSpotIds(content.getId())).isEmpty();
            then(naverBlogClient).shouldHaveNoInteractions();

            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("후기", "https://blog.naver.com/a/1", "20260701")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 1, 1, List.of(), List.of(),
                    List.of(new VerifyResult.VerifiedCourse(1, List.of(5L, 6L), 1, List.of(0))), List.of()));
            routeVerificationService.verify(content.getId());

            // when / then — 랭킹 순 코스 스팟 시퀀스
            assertThat(routeVerificationService.cachedCourseSpotIds(content.getId()))
                    .containsExactly(List.of(5L, 6L));
        }

        @Test
        @DisplayName("검색은 멀티 쿼리(성지순례·성지 후기)를 display 50으로 부르고 링크 중복은 합친다")
        void searchesWithExpandedMultiQuery() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(eq(content.getTitle() + " 성지순례"), eq(50))).willReturn(List.of(
                    new NaverBlogClient.BlogItem("겹침후기", "https://blog.naver.com/dup/1", "20260701")));
            given(naverBlogClient.search(eq(content.getTitle() + " 성지 후기"), eq(50))).willReturn(List.of(
                    new NaverBlogClient.BlogItem("겹침후기", "https://blog.naver.com/dup/1", "20260701"),
                    new NaverBlogClient.BlogItem("추가후기", "https://blog.naver.com/extra/2", "20260702")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 2, 2, List.of(), List.of(), List.of(), List.of()));

            // when
            routeVerificationService.verify(content.getId());

            // then — 두 쿼리 결과를 링크 기준으로 합쳐 중복 없이 본문 조회
            then(naverBlogClient).should(times(1)).search(eq(content.getTitle() + " 성지순례"), eq(50));
            then(naverBlogClient).should(times(1)).search(eq(content.getTitle() + " 성지 후기"), eq(50));
            then(postFetcher).should(times(2)).fetchText(anyString());
        }

        @Test
        @DisplayName("언급(spotMentions)의 postIndexes를 출처(제목·링크·날짜)로 매핑한다")
        void mapsMentionSources() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("최신후기", "https://blog.naver.com/new/1", "20260705"),
                    new NaverBlogClient.BlogItem("감상평", "https://blog.naver.com/essay/2", "20250101")
            ));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 2, 1,
                    List.of(new VerifyResult.SpotMention(1L, 2, List.of(0, 1))),
                    List.of(), List.of(), List.of()));

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then — 언급 출처 2건 (visited 아니어도 언급 출처로 노출)
            assertThat(response.spotMentions()).hasSize(1);
            var mention = response.spotMentions().get(0);
            assertThat(mention.count()).isEqualTo(2);
            assertThat(mention.sources()).extracting(RouteVerificationResponse.Source::title)
                    .containsExactly("최신후기", "감상평");
        }

        @Test
        @DisplayName("스팟별 팁의 postIndex를 출처(제목·링크·날짜)로 매핑한다")
        void mapsSpotTipSources() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("팁후기", "https://blog.naver.com/tip/1", "20260701")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 1, 1, List.of(), List.of(), List.of(),
                    List.of(new VerifyResult.SpotTips(1L,
                            List.of(new VerifyResult.SpotTips.Tip("한정 에마는 오전에 소진된다", 0))))));

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then
            assertThat(response.spotTips()).hasSize(1);
            var tips = response.spotTips().get(0);
            assertThat(tips.spotId()).isEqualTo(1L);
            assertThat(tips.tips().get(0).tip()).isEqualTo("한정 에마는 오전에 소진된다");
            assertThat(tips.tips().get(0).source().title()).isEqualTo("팁후기");
        }

        @Test
        @DisplayName("코스가 없는 빈약 결과는 30분 뒤 재수집된다 — 영구 고정 방지")
        void thinResultExpiresInThirtyMinutes() {
            // given — 후기는 있으나 코스 0 (빈약 표본)
            Content content = savedContentWithSpot();
            nowAt(Instant.parse("2026-07-08T10:00:00Z"));
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("후기", "https://blog.naver.com/a/1", "20260701")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 1, 1, List.of(), List.of(), List.of(), List.of()));
            routeVerificationService.verify(content.getId());

            // when — 31분 뒤 재조회
            nowAt(Instant.parse("2026-07-08T10:31:00Z"));
            routeVerificationService.verify(content.getId());

            // then — 만료돼 재수집
            then(aiRouteVerifyClient).should(times(2)).verify(any());
        }

        @Test
        @DisplayName("코스가 있는 결과는 6시간 안에는 캐시를 쓰고, 지나면 재수집한다")
        void richResultExpiresInSixHours() {
            // given
            Content content = savedContentWithSpot();
            nowAt(Instant.parse("2026-07-08T10:00:00Z"));
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("후기", "https://blog.naver.com/a/1", "20260701")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.of("본문 ".repeat(100)));
            given(aiRouteVerifyClient.verify(any())).willReturn(new VerifyResult(
                    "openai", 1, 1, List.of(), List.of(),
                    List.of(new VerifyResult.VerifiedCourse(1, List.of(5L, 6L), 2, List.of(0))), List.of()));
            routeVerificationService.verify(content.getId());

            // when / then — 5시간 뒤엔 캐시, 7시간 뒤엔 재수집
            nowAt(Instant.parse("2026-07-08T15:00:00Z"));
            routeVerificationService.verify(content.getId());
            then(aiRouteVerifyClient).should(times(1)).verify(any());

            nowAt(Instant.parse("2026-07-08T17:01:00Z"));
            routeVerificationService.verify(content.getId());
            then(aiRouteVerifyClient).should(times(2)).verify(any());
        }

        @Test
        @DisplayName("본문을 하나도 못 얻으면 ai를 부르지 않고 빈 결과를 반환한다")
        void skipsAiWhenNoTexts() {
            // given
            Content content = savedContentWithSpot();
            given(naverBlogClient.enabled()).willReturn(true);
            given(naverBlogClient.search(anyString(), anyInt())).willReturn(List.of(
                    new NaverBlogClient.BlogItem("후기1", "https://blog.naver.com/a/1", "20260701")));
            given(postFetcher.fetchText(anyString())).willReturn(Optional.empty());

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then
            assertThat(response.available()).isTrue();
            assertThat(response.usedPostCount()).isZero();
            then(aiRouteVerifyClient).shouldHaveNoInteractions();
        }
    }
}

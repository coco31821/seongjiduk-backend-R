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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
                    List.of(new VerifyResult.SpotMention(1L, 2)),
                    List.of(new VerifyResult.VerifiedPair(1L, 2L, 2)), List.of()));

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
                    List.of(new VerifyResult.VerifiedCourse(1, List.of(5L, 6L), 2, List.of(0, 1)))));

            // when
            RouteVerificationResponse response = routeVerificationService.verify(content.getId());

            // then — 최신순 정렬로 index0=최신후기
            assertThat(response.courses()).hasSize(1);
            var course = response.courses().get(0);
            assertThat(course.spotIds()).containsExactly(5L, 6L);
            assertThat(course.sources()).extracting(RouteVerificationResponse.VerifiedCourse.Source::title)
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
                    "openai", 1, 1, List.of(), List.of(new VerifyResult.VerifiedPair(1L, 2L, 1)), List.of()));

            // when
            routeVerificationService.verify(content.getId());
            routeVerificationService.verify(content.getId());

            // then
            then(naverBlogClient).should(times(1)).search(anyString(), anyInt());
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
                    List.of(new VerifyResult.VerifiedCourse(1, List.of(5L, 6L), 1, List.of(0)))));
            routeVerificationService.verify(content.getId());

            // when / then — 랭킹 순 코스 스팟 시퀀스
            assertThat(routeVerificationService.cachedCourseSpotIds(content.getId()))
                    .containsExactly(List.of(5L, 6L));
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

package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.content.service.ContentService;
import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import com.sungjiduk.backend.spot.infra.AiRouteVerifyClient;
import com.sungjiduk.backend.spot.infra.BlogPostFetcher;
import com.sungjiduk.backend.spot.infra.NaverBlogClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// redis
import org.springframework.data.redis.core.RedisTemplate;

@Service
@Transactional(readOnly = true)
public class RouteVerificationService {

    private final ContentRepository contentRepository;
    private final ContentService contentService;
    private final NaverBlogClient naverBlogClient;
    private final BlogPostFetcher postFetcher;
    private final AiRouteVerifyClient aiRouteVerifyClient;
    private final java.time.Clock clock;
    private final RedisTemplate<String, Object> redisTemplate;

    public RouteVerificationService(
            ContentRepository contentRepository,
            ContentService contentService,
            NaverBlogClient naverBlogClient,
            BlogPostFetcher postFetcher,
            AiRouteVerifyClient aiRouteVerifyClient,
            java.time.Clock clock,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.contentRepository = contentRepository;
        this.contentService = contentService;
        this.naverBlogClient = naverBlogClient;
        this.postFetcher = postFetcher;
        this.aiRouteVerifyClient = aiRouteVerifyClient;
        this.clock = clock;
        this.redisTemplate = redisTemplate;
    }

    private static final int SEARCH_COUNT = 50;
    private static final int MAX_POSTS = 12;
    /** 검색 recall 확대용 멀티 쿼리 — 결과는 링크 기준으로 합쳐 중복 제거한다. */
    private static final java.util.List<String> QUERY_SUFFIXES = java.util.List.of(" 성지순례", " 성지 후기");

    /**
     * 작품별 검증 결과 캐시 — 외부 API·LLM 비용 절약. 빈 결과(usedPostCount=0)는 캐시하지 않는다.
     * TTL: 코스가 있으면 6시간, 후기는 있으나 코스가 없으면(빈약 표본) 30분 뒤 재수집 — 영구 고정 방지.
     */
    private static final java.time.Duration RICH_TTL = java.time.Duration.ofHours(6);
    private static final java.time.Duration THIN_TTL = java.time.Duration.ofMinutes(30);

    private String routeKey(Long contentId) {
        return "route-verification:content:" + contentId;
    }   // example. Redis key 값 : route-verification:content:10



    private RouteVerificationResponse cachedResponse(Long contentId) {
        Object cached = redisTemplate.opsForValue().get(routeKey(contentId));   // java 객체를 그대로 넘김.

        if (cached instanceof RouteVerificationResponse response) {
            return response;
        }

        return null;
    }   // Redis TTL이 끝난 key는 Redis가 알아서 제거하거나 조회 불가 상태로 만듦.

    private void cacheResponse(Long contentId, RouteVerificationResponse response) {
        java.time.Duration ttl = response.courses().isEmpty() ? THIN_TTL : RICH_TTL;
        redisTemplate.opsForValue().set(routeKey(contentId), response, ttl);
    }

    public RouteVerificationResponse verify(Long contentId) {
        RouteVerificationResponse cached = cachedResponse(contentId);
        if (cached != null) {
            return cached;
        }
        if (!naverBlogClient.enabled()) {
            return RouteVerificationResponse.unavailable(contentId);
        }
        var content = contentRepository.findByIdOrThrow(contentId);
        // 한국 블로그는 한국어 표기를 쓰므로 koreanName 포함 목록(설명 캐시)을 재사용해 매칭률을 높인다
        var spots = contentService.findContentSpots(contentId).spots();

        var items = new java.util.ArrayList<NaverBlogClient.BlogItem>();
        for (String suffix : QUERY_SUFFIXES) {
            items.addAll(naverBlogClient.search(content.getTitle() + suffix, SEARCH_COUNT));
        }
        // 최근 방문 후기 우선 (postdate desc) — 최신 코스가 상위로
        items.sort(java.util.Comparator.comparing(
                (NaverBlogClient.BlogItem it) -> it.postdate() == null ? "" : it.postdate()).reversed());
        // 링크 중복 제거 후 본문 확보(모바일 뷰) — 원문은 이 요청 스코프에서만 사용
        java.util.List<AiRouteVerifyClient.VerifyRequest.BlogPost> posts = new java.util.ArrayList<>();
        java.util.Set<String> seenLinks = new java.util.HashSet<>();
        for (var item : items) {
            if (posts.size() >= MAX_POSTS || item.link() == null || !seenLinks.add(item.link())) {
                continue;
            }
            postFetcher.fetchText(item.link())
                    .ifPresent(text -> posts.add(new AiRouteVerifyClient.VerifyRequest.BlogPost(
                            item.title(), text, item.postdate(), item.link())));
        }
        if (posts.isEmpty()) {
            return new RouteVerificationResponse(contentId, true, items.size(), 0,
                    java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
        }

        try {
            var result = aiRouteVerifyClient.verify(new AiRouteVerifyClient.VerifyRequest(
                    new AiRouteVerifyClient.VerifyRequest.Content(content.getId(), content.getTitle()),
                    spots.stream().map(spot -> new AiRouteVerifyClient.VerifyRequest.RouteSpot(
                            spot.id(), spot.name(), spot.koreanName())).toList(),
                    posts));
            if (result == null) {
                return new RouteVerificationResponse(contentId, true, posts.size(), 0,
                        java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
            }
            RouteVerificationResponse response = new RouteVerificationResponse(
                    contentId, true, result.postCount(), result.usedPostCount(),
                    result.spotMentions() == null ? java.util.List.of() : result.spotMentions().stream()
                            .map(m -> new RouteVerificationResponse.SpotMention(
                                    m.spotId(), m.count(), toSources(m.postIndexes(), posts))).toList(),
                    result.verifiedPairs() == null ? java.util.List.of() : result.verifiedPairs().stream()
                            .map(v -> new RouteVerificationResponse.VerifiedPair(v.fromSpotId(), v.toSpotId(), v.count())).toList(),
                    result.courses() == null ? java.util.List.of() : result.courses().stream()
                            .map(course -> new RouteVerificationResponse.VerifiedCourse(
                                    course.rank(), course.spotIds(), course.supportCount(),
                                    toSources(course.postIndexes(), posts)))
                            .toList(),
                    result.spotTips() == null ? java.util.List.of() : result.spotTips().stream()
                            .map(spotTip -> new RouteVerificationResponse.SpotTips(
                                    spotTip.spotId(),
                                    spotTip.tips() == null ? java.util.List.<RouteVerificationResponse.SpotTips.TipEntry>of()
                                            : spotTip.tips().stream()
                                                    .map(tip -> new RouteVerificationResponse.SpotTips.TipEntry(
                                                            tip.tip(), sourceAt(tip.postIndex(), posts)))
                                                    .toList()))
                            .toList());
            if (response.usedPostCount() > 0) {
                cacheResponse(contentId, response);
            }
            return response;
        } catch (RuntimeException e) {
            return new RouteVerificationResponse(contentId, true, posts.size(), 0,
                    java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
        }
    }

    /**
     * 캐시된 검증 코스의 스팟 시퀀스(랭킹 순) — 일정 생성이 백본으로 쓴다.
     * 캐시가 없으면 빈 목록: 검증은 느린 파이프라인이라 여기서 발화시키지 않는다.
     */
    public java.util.List<java.util.List<Long>> cachedCourseSpotIds(Long contentId) {
        RouteVerificationResponse cached = cachedResponse(contentId);
        if (cached == null || cached.courses() == null) {
            return java.util.List.of();
        }
        return cached.courses().stream()
                .map(RouteVerificationResponse.VerifiedCourse::spotIds)
                .toList();
    }

    /** ai가 준 포스트 인덱스를 출처(제목·링크·작성일)로 매핑한다. 범위 밖 인덱스는 무시. */
    private java.util.List<RouteVerificationResponse.Source> toSources(
            java.util.List<Integer> postIndexes,
            java.util.List<AiRouteVerifyClient.VerifyRequest.BlogPost> posts) {
        if (postIndexes == null) {
            return java.util.List.of();
        }
        return postIndexes.stream()
                .filter(i -> i != null && i >= 0 && i < posts.size())
                .map(i -> {
                    var post = posts.get(i);
                    return new RouteVerificationResponse.Source(post.title(), post.link(), post.postdate());
                }).toList();
    }

    /** 단일 포스트 인덱스 → 출처 (범위 밖이면 null) */
    private RouteVerificationResponse.Source sourceAt(
            Integer postIndex,
            java.util.List<AiRouteVerifyClient.VerifyRequest.BlogPost> posts) {
        if (postIndex == null || postIndex < 0 || postIndex >= posts.size()) {
            return null;
        }
        var post = posts.get(postIndex);
        return new RouteVerificationResponse.Source(post.title(), post.link(), post.postdate());
    }
}

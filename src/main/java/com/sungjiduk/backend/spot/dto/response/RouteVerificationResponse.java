package com.sungjiduk.backend.spot.dto.response;

import java.util.List;

/** 블로그 후기 기반 동선 검증 결과 (키 미설정 시 available=false). */
public record RouteVerificationResponse(
        Long contentId,
        boolean available,
        int postCount,
        int usedPostCount,
        List<SpotMention> spotMentions,
        List<VerifiedPair> verifiedPairs
) {
    public record SpotMention(Long spotId, int count) {
    }

    public record VerifiedPair(Long fromSpotId, Long toSpotId, int count) {
    }

    public static RouteVerificationResponse unavailable(Long contentId) {
        return new RouteVerificationResponse(contentId, false, 0, 0, List.of(), List.of());
    }
}

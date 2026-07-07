package com.sungjiduk.backend.spot.dto.response;

import java.util.List;

/** 블로그 후기 기반 동선 검증 결과 (키 미설정 시 available=false). */
public record RouteVerificationResponse(
        Long contentId,
        boolean available,
        int postCount,
        int usedPostCount,
        List<SpotMention> spotMentions,
        List<VerifiedPair> verifiedPairs,
        List<VerifiedCourse> courses
) {
    /** 후기 출처 (제목·링크·작성일) — 코스·언급이 공유한다. */
    public record Source(String title, String link, String postdate) {
    }

    public record VerifiedCourse(
            int rank,
            List<Long> spotIds,
            int supportCount,
            List<Source> sources
    ) {
    }

    /** 언급은 전체 확보 후기 기준(감상평 포함), 출처 링크를 함께 담는다. */
    public record SpotMention(Long spotId, int count, List<Source> sources) {
    }

    public record VerifiedPair(Long fromSpotId, Long toSpotId, int count) {
    }

    public static RouteVerificationResponse unavailable(Long contentId) {
        return new RouteVerificationResponse(contentId, false, 0, 0, List.of(), List.of(), List.of());
    }
}

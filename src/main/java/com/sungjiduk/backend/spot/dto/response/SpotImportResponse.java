package com.sungjiduk.backend.spot.dto.response;

/**
 * 성지 임포트 결과 요약. 부분 실패는 카운트로 표시(전체 실패 아님).
 */
public record SpotImportResponse(
        Long contentId,
        long bangumiId,
        int created,
        int updated,
        int geocodeFallback,
        int failed,
        int total
) {
}

package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;

import java.time.Duration;

/**
 * 작품별 검증 결과 캐시 — 외부 API·LLM 비용 절약용.
 * mode로 구현을 토글한다(§7 가드와 동일 규약):
 * - redis : 인스턴스 공유 분산 캐시(운영 기본, 장애 시 fail-open)
 * - local : 인메모리 단일 캐시(Redis 없이 동작 — 로컬·테스트). TTL은 주입된 Clock 기준.
 */
public interface RouteVerificationCache {

    /** 유효한 캐시 값(없거나 만료면 null). */
    RouteVerificationResponse get(Long contentId);

    /** 값을 ttl 동안 캐시한다. */
    void put(Long contentId, RouteVerificationResponse response, Duration ttl);
}

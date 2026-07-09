package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 단일 인스턴스 캐시 — Redis 없이 동작(로컬·테스트).
 * 만료는 주입된 {@link Clock} 기준이라, 테스트가 시계를 앞당겨 TTL 만료를 검증할 수 있다.
 */
public class InMemoryRouteVerificationCache implements RouteVerificationCache {

    private record Entry(RouteVerificationResponse response, Instant expiresAt) {}

    private final Clock clock;
    private final Map<Long, Entry> store = new ConcurrentHashMap<>();

    public InMemoryRouteVerificationCache(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RouteVerificationResponse get(Long contentId) {
        Entry entry = store.get(contentId);
        if (entry == null) {
            return null;
        }
        if (!clock.instant().isBefore(entry.expiresAt())) {   // now >= expiresAt → 만료
            store.remove(contentId, entry);
            return null;
        }
        return entry.response();
    }

    @Override
    public void put(Long contentId, RouteVerificationResponse response, Duration ttl) {
        store.put(contentId, new Entry(response, clock.instant().plus(ttl)));
    }
}

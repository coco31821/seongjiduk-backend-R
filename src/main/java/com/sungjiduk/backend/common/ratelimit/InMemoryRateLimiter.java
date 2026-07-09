package com.sungjiduk.backend.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 고정 윈도우 레이트리미터의 인메모리 레퍼런스 구현.
 * 단일 인스턴스 한정(프로세스 로컬). Redis 구현({@link RedisRateLimiter})이 미러하는 기준 의미이자,
 * Redis 비활성 시 폴백으로 쓴다. {@link Clock} 주입으로 윈도우 경과를 테스트 가능하게 한다.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private record Counter(long windowStartMs, int count) {}

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0) {
            return false;
        }
        long now = clock.millis();
        long windowMs = window.toMillis();
        long currentWindowStart = now - (now % windowMs);
        boolean[] allowed = {false};
        counters.compute(key, (k, cur) -> {
            // 다른 윈도우로 넘어갔으면 카운트 리셋
            if (cur == null || cur.windowStartMs() != currentWindowStart) {
                allowed[0] = true;
                return new Counter(currentWindowStart, 1);
            }
            if (cur.count() < limit) {
                allowed[0] = true;
                return new Counter(currentWindowStart, cur.count() + 1);
            }
            allowed[0] = false;
            return cur;
        });
        return allowed[0];
    }
}

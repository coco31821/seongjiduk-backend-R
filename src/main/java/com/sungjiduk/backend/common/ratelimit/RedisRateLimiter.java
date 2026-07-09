package com.sungjiduk.backend.common.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/**
 * 분산 고정 윈도우 레이트리미터(Redis). 인스턴스를 넘어 <b>공유 한도</b>를 세므로,
 * 수평 확장해도 외부 API 키가 429/과금을 넘지 않는다.
 *
 * <p>원자성: {@code INCR}+{@code PEXPIRE}+비교를 단일 Lua로 실행해 경합/누수(EXPIRE 유실)를 없앤다.
 * 윈도우 버킷은 벽시계로 만들며, 의미는 {@link InMemoryRateLimiter}(테스트된 레퍼런스)와 동일하다.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
            + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end "
            + "if c <= tonumber(ARGV[1]) then return 1 else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0) {
            return false;
        }
        long windowMs = window.toMillis();
        long now = System.currentTimeMillis();
        long windowStart = now - (now % windowMs);
        String bucketKey = "rl:" + key + ":" + windowStart;
        Long allowed = redis.execute(SCRIPT, List.of(bucketKey),
                String.valueOf(limit), String.valueOf(windowMs));
        return allowed != null && allowed == 1L;
    }
}

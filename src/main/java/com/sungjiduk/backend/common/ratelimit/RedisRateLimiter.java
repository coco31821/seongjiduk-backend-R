package com.sungjiduk.backend.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/**
 * 분산 Token Bucket 레이트리미터(Redis). 인스턴스를 넘어 <b>공유 한도</b>를 세므로,
 * 수평 확장해도 외부 API 키가 429/과금을 넘지 않는다.
 *
 * <p>Redis TIME·HMSET·PEXPIRE를 하나의 Lua로 실행한다. JVM clock skew와 fixed-window 경계의
 * 2배 burst를 피하면서, window 동안 limit개를 안정적으로 refill한다.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local t=redis.call('TIME'); local now=t[1]*1000+math.floor(t[2]/1000) "
            + "local cap=tonumber(ARGV[1]); local period=tonumber(ARGV[2]) "
            + "local v=redis.call('HMGET', KEYS[1], 'tokens', 'updated') "
            + "local tokens=tonumber(v[1]) or cap; local updated=tonumber(v[2]) or now "
            + "tokens=math.min(cap, tokens + ((now-updated)*cap/period)) "
            + "local allowed=0; if tokens >= 1 then tokens=tokens-1; allowed=1 end "
            + "redis.call('HSET', KEYS[1], 'tokens', tokens, 'updated', now) "
            + "redis.call('PEXPIRE', KEYS[1], period*2) return allowed",
            Long.class);

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

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
        if (windowMs <= 0) {
            return false;
        }
        String bucketKey = "rl:v2:" + key;
        try {
            Long allowed = redis.execute(SCRIPT, List.of(bucketKey),
                    String.valueOf(limit), String.valueOf(windowMs));
            return allowed != null && allowed == 1L;
        } catch (DataAccessException e) {
            // fail-open: Redis 장애가 서비스 호출을 막지 않도록 통과시킨다(보호는 일시 상실).
            log.warn("RateLimiter Redis 오류 — fail-open 통과: {}", e.getMessage());
            return true;
        }
    }
}

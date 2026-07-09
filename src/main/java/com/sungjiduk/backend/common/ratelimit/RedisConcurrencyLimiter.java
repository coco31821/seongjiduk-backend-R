package com.sungjiduk.backend.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 분산 동시성 리미터(Redis ZSET 세마포어). 인스턴스 전체가 공유하는 동시 실행 한도.
 *
 * <p>ZSET에 (score=획득시각, member=토큰)으로 진입자를 기록한다. 획득 시 Lua로
 * ① 오래된(=크래시로 반납 못 한) 진입자를 stale timeout 기준으로 청소 → ② 정원 미만이면 자기 토큰 추가.
 * 이 청소 덕에 홀더가 죽어도 timeout 뒤 자리가 자동 회수된다. 반납은 {@code ZREM}(멱등).
 * 자리가 없으면 짧게 재시도하며 {@code wait}까지 기다린다.
 */
public class RedisConcurrencyLimiter implements ConcurrencyLimiter {

    /** ARGV: [max, staleCutoffMs, nowMs, token, keyTtlMs] */
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[2]) "
            + "local n = redis.call('ZCARD', KEYS[1]) "
            + "if n < tonumber(ARGV[1]) then "
            + "  redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4]) "
            + "  redis.call('PEXPIRE', KEYS[1], ARGV[5]) "
            + "  return 1 "
            + "else return 0 end",
            Long.class);

    private static final Logger log = LoggerFactory.getLogger(RedisConcurrencyLimiter.class);

    /** 크래시한 홀더의 자리를 회수하기까지의 최대 점유 시간(안전망). */
    private static final long STALE_TIMEOUT_MS = Duration.ofMinutes(2).toMillis();
    private static final long RETRY_INTERVAL_MS = 50;

    /** fail-open 시 반환하는 무동작 permit(반납할 것 없음). */
    private static final Permit NO_OP = () -> { };

    private final StringRedisTemplate redis;

    public RedisConcurrencyLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<Permit> acquire(String key, int maxConcurrent, Duration wait) {
        if (maxConcurrent <= 0) {
            return Optional.empty();
        }
        String zkey = "sem:" + key;
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + Math.max(0, wait.toMillis());
        try {
            do {
                if (tryAcquireOnce(zkey, maxConcurrent, token)) {
                    return Optional.of(new RedisPermit(zkey, token));
                }
                if (System.currentTimeMillis() >= deadline) {
                    break;
                }
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } while (System.currentTimeMillis() < deadline);
            return Optional.empty();
        } catch (DataAccessException e) {
            // fail-open: Redis 장애가 AI 호출을 막지 않도록 통과(동시성 보호는 일시 상실).
            log.warn("ConcurrencyLimiter Redis 오류 — fail-open 통과: {}", e.getMessage());
            return Optional.of(NO_OP);
        }
    }

    private boolean tryAcquireOnce(String zkey, int max, String token) {
        long now = System.currentTimeMillis();
        Long granted = redis.execute(ACQUIRE, List.of(zkey),
                String.valueOf(max),
                String.valueOf(now - STALE_TIMEOUT_MS),
                String.valueOf(now),
                token,
                String.valueOf(STALE_TIMEOUT_MS * 2));
        return granted != null && granted == 1L;
    }

    /** 멱등 반납: 자기 토큰만 ZREM. */
    private final class RedisPermit implements Permit {
        private final String zkey;
        private final String token;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private RedisPermit(String zkey, String token) {
            this.zkey = zkey;
            this.token = token;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                redis.opsForZSet().remove(zkey, token);
            }
        }
    }
}

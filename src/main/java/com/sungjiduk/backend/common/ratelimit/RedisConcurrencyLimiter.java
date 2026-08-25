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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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

    /** Redis TIME을 사용해 replica JVM 시계 오차를 제거한다. */
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>(
            "local t=redis.call('TIME'); local now=t[1]*1000+math.floor(t[2]/1000) "
            + "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now-tonumber(ARGV[2])) "
            + "local n = redis.call('ZCARD', KEYS[1]) "
            + "if n < tonumber(ARGV[1]) then "
            + "  redis.call('ZADD', KEYS[1], now, ARGV[3]) "
            + "  redis.call('PEXPIRE', KEYS[1], ARGV[4]) "
            + "  return 1 "
            + "else return 0 end",
            Long.class);
    private static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>(
            "if redis.call('ZSCORE', KEYS[1], ARGV[1]) == false then return 0 end "
            + "local t=redis.call('TIME'); local now=t[1]*1000+math.floor(t[2]/1000) "
            + "redis.call('ZADD', KEYS[1], 'XX', now, ARGV[1]); redis.call('PEXPIRE', KEYS[1], ARGV[2]); return 1",
            Long.class);

    private static final Logger log = LoggerFactory.getLogger(RedisConcurrencyLimiter.class);


    /** fail-open 시 반환하는 무동작 permit(반납할 것 없음). */
    private static final Permit NO_OP = () -> { };

    private final StringRedisTemplate redis;
    private final long leaseMs;
    private final long heartbeatMs;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "redis-permit-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public RedisConcurrencyLimiter(StringRedisTemplate redis) {
        this(redis, 120_000, 20_000);
    }
    public RedisConcurrencyLimiter(StringRedisTemplate redis, long leaseMs, long heartbeatMs) {
        this.redis = redis;
        this.leaseMs = leaseMs;
        this.heartbeatMs = Math.min(heartbeatMs, Math.max(1_000, leaseMs / 2));
    }

    @Override
    public Optional<Permit> acquire(String key, int maxConcurrent, Duration wait) {
        if (maxConcurrent <= 0) {
            return Optional.empty();
        }
        String zkey = "sem:" + key;
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0, wait.toMillis()));
        long delayMs = 20;
        try {
            do {
                if (tryAcquireOnce(zkey, maxConcurrent, token)) {
                    return Optional.of(new RedisPermit(zkey, token));
                }
                if (System.nanoTime() >= deadline) {
                    break;
                }
                try {
                    Thread.sleep(delayMs + ThreadLocalRandom.current().nextLong(0, Math.max(1, delayMs / 3)));
                    delayMs = Math.min(250, delayMs * 2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } while (System.nanoTime() < deadline);
            return Optional.empty();
        } catch (DataAccessException e) {
            // fail-open: Redis 장애가 AI 호출을 막지 않도록 통과(동시성 보호는 일시 상실).
            log.warn("ConcurrencyLimiter Redis 오류 — fail-open 통과: {}", e.getMessage());
            return Optional.of(NO_OP);
        }
    }

    private boolean tryAcquireOnce(String zkey, int max, String token) {
        Long granted = redis.execute(ACQUIRE, List.of(zkey),
                String.valueOf(max),
                String.valueOf(leaseMs),
                token,
                String.valueOf(leaseMs * 2));
        return granted != null && granted == 1L;
    }

    /** 멱등 반납: 자기 토큰만 ZREM. */
    private final class RedisPermit implements Permit {
        private final String zkey;
        private final String token;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final ScheduledFuture<?> heartbeat;

        private RedisPermit(String zkey, String token) {
            this.zkey = zkey;
            this.token = token;
            this.heartbeat = heartbeatExecutor.scheduleAtFixedRate(this::renew, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
        }

        private void renew() {
            if (released.get()) return;
            try {
                Long renewed = redis.execute(RENEW, List.of(zkey), token, String.valueOf(leaseMs * 2));
                if (renewed == null || renewed == 0L) log.warn("Redis permit lease lost: {}", zkey);
            } catch (DataAccessException e) {
                log.warn("Redis permit heartbeat failed: {}", e.getMessage());
            }
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                heartbeat.cancel(false);
                try {
                    redis.opsForZSet().remove(zkey, token);
                } catch (DataAccessException e) {
                    // release는 best-effort: 성공한 AI 응답을 Redis 장애로 실패시키지 않는다.
                    log.warn("Redis permit release failed: {}", e.getMessage());
                }
            }
        }
    }
}

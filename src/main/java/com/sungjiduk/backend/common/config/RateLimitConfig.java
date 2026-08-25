package com.sungjiduk.backend.common.config;

import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter;
import com.sungjiduk.backend.common.ratelimit.InMemoryConcurrencyLimiter;
import com.sungjiduk.backend.common.ratelimit.InMemoryRateLimiter;
import com.sungjiduk.backend.common.ratelimit.NoOpConcurrencyLimiter;
import com.sungjiduk.backend.common.ratelimit.NoOpRateLimiter;
import com.sungjiduk.backend.common.ratelimit.RateLimiter;
import com.sungjiduk.backend.common.ratelimit.RedisConcurrencyLimiter;
import com.sungjiduk.backend.common.ratelimit.RedisRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

/**
 * 처리량 가드 빈을 mode에 따라 선택 조립한다.
 * - redis: 분산(인스턴스 공유 한도) — 운영 기본
 * - local: 인메모리(단일 인스턴스) — Redis 없이 동작하는 폴백/테스트
 * - off  : 무제한 — Redis 도입 "이전"의 무방비 상태 재현(벤치 대조군)
 */
@Configuration
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    @Bean
    public ConcurrencyLimiter concurrencyLimiter(RedisGuardProperties props,
                                                 ObjectProvider<StringRedisTemplate> redis) {
        String mode = props.getConcurrencyLimitMode().name().toLowerCase();
        log.info("ConcurrencyLimiter mode={}", mode);
        return switch (mode) {
            case "off" -> new NoOpConcurrencyLimiter();
            case "local" -> new InMemoryConcurrencyLimiter();
            default -> new RedisConcurrencyLimiter(requireRedis(redis, mode), props.getAiLeaseMs(), props.getAiHeartbeatMs());
        };
    }

    @Bean
    public RateLimiter rateLimiter(RedisGuardProperties props,
                                   ObjectProvider<StringRedisTemplate> redis) {
        String mode = props.getRateLimitMode().name().toLowerCase();
        log.info("RateLimiter mode={}", mode);
        return switch (mode) {
            case "off" -> new NoOpRateLimiter();
            case "local" -> new InMemoryRateLimiter(Clock.systemUTC());
            default -> new RedisRateLimiter(requireRedis(redis, mode));
        };
    }

    private static StringRedisTemplate requireRedis(ObjectProvider<StringRedisTemplate> redis, String mode) {
        StringRedisTemplate template = redis.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException(
                    "seongjiduk.redis-guard.mode=" + mode + " 인데 StringRedisTemplate 빈이 없습니다. "
                    + "Redis 설정을 확인하거나 mode=local/off로 두세요.");
        }
        return template;
    }
}

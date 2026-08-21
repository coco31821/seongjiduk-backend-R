package com.sungjiduk.backend.common.config;

import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.spot.service.cache.InMemoryRouteVerificationCache;
import com.sungjiduk.backend.spot.service.cache.RedisRouteVerificationCache;
import com.sungjiduk.backend.spot.service.cache.RouteVerificationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

/**
 * 검증 결과 캐시 구현을 §7 가드와 동일한 mode 규약으로 선택한다.
 * - redis(기본): 분산 캐시(운영) — 장애 시 fail-open
 * - local/off : 인메모리(단일 인스턴스) — Redis 없이 동작(로컬·테스트), Clock 기준 TTL
 */
@Configuration
public class RouteCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(RouteCacheConfig.class);

    @Bean
    public RouteVerificationCache routeVerificationCache(
            RedisGuardProperties props,
            Clock clock,
            ObjectProvider<StringRedisTemplate> redis,
            ObjectMapper objectMapper) {
        String mode = props.getCacheMode().name().toLowerCase();
        log.info("RouteVerificationCache mode={}", mode);
        if ("redis".equals(mode)) {
            StringRedisTemplate template = redis.getIfAvailable();
            if (template == null) {
                throw new IllegalStateException(
                        "seongjiduk.redis-guard.mode=redis 인데 StringRedisTemplate 빈이 없습니다. "
                        + "Redis 설정을 확인하거나 mode=local/off로 두세요.");
            }
            return new RedisRouteVerificationCache(template, objectMapper);
        }
        if ("off".equals(mode)) {
            return new com.sungjiduk.backend.spot.service.cache.NoOpRouteVerificationCache();
        }
        return new InMemoryRouteVerificationCache(clock);
    }
}

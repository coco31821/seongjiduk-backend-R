package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/**
 * Redis 분산 캐시 — 인스턴스 간 공유(운영 기본).
 * 캐시는 비용 절약 수단일 뿐이라 Redis 장애 시 fail-open: get은 미스로 간주하고 put은 건너뛴다.
 * (검증 결과 자체는 이미 반환됐고, 다음 요청이 재계산한다.)
 */
public class RedisRouteVerificationCache implements RouteVerificationCache {

    private static final Logger log = LoggerFactory.getLogger(RedisRouteVerificationCache.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRouteVerificationCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String routeKey(Long contentId) {
        return "route-verification:content:" + contentId;   // 예: route-verification:content:10
    }

    @Override
    public RouteVerificationResponse get(Long contentId) {
        try {
            Object cached = redisTemplate.opsForValue().get(routeKey(contentId));
            return (cached instanceof RouteVerificationResponse response) ? response : null;
        } catch (RuntimeException e) {
            log.warn("route-verification 캐시 조회 실패(fail-open, 재계산) contentId={} : {}", contentId, e.toString());
            return null;
        }
    }

    @Override
    public void put(Long contentId, RouteVerificationResponse response, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(routeKey(contentId), response, ttl);
        } catch (RuntimeException e) {
            log.warn("route-verification 캐시 저장 실패(fail-open, 저장 생략) contentId={} : {}", contentId, e.toString());
        }
    }
}

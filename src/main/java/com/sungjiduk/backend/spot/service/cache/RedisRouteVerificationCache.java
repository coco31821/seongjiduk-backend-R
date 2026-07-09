package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Redis 분산 캐시 — 인스턴스 간 공유(운영 기본).
 * 직렬화는 ObjectMapper로 명시적 JSON 문자열 왕복 — RedisTemplate<String,Object>의
 * 범용 직렬화는 타입 정보(@class) 없이는 역직렬화가 Map으로 떨어져 instanceof에 항상
 * 실패(캐시 무한 미스 = 매 조회마다 네이버+GPT 재계산)하던 사고의 재발 방지.
 * 캐시는 비용 절약 수단일 뿐이라 Redis/직렬화 장애 시 fail-open: get은 미스, put은 생략.
 */
public class RedisRouteVerificationCache implements RouteVerificationCache {

    private static final Logger log = LoggerFactory.getLogger(RedisRouteVerificationCache.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRouteVerificationCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String routeKey(Long contentId) {
        return "route-verification:content:" + contentId;   // 예: route-verification:content:10
    }

    @Override
    public RouteVerificationResponse get(Long contentId) {
        try {
            String json = redisTemplate.opsForValue().get(routeKey(contentId));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, RouteVerificationResponse.class);
        } catch (RuntimeException e) {
            log.warn("route-verification 캐시 조회 실패(fail-open, 재계산) contentId={} : {}", contentId, e.toString());
            return null;
        }
    }

    @Override
    public void put(Long contentId, RouteVerificationResponse response, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(routeKey(contentId), json, ttl);
        } catch (RuntimeException e) {
            log.warn("route-verification 캐시 저장 실패(fail-open, 저장 생략) contentId={} : {}", contentId, e.toString());
        }
    }
}

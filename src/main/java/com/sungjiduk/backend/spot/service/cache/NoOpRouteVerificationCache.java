package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import java.time.Duration;

/** 부하 실험의 진짜 no-cache 대조군. */
public class NoOpRouteVerificationCache implements RouteVerificationCache {
    @Override public RouteVerificationResponse get(Long contentId) { return null; }
    @Override public void put(Long contentId, RouteVerificationResponse response, Duration ttl) { }
}

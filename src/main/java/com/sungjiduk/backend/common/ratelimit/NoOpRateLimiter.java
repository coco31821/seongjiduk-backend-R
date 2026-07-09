package com.sungjiduk.backend.common.ratelimit;

import java.time.Duration;

/** 제한 없음(항상 허용). 가드 off 모드 — Redis 도입 "이전"의 무제한 동작을 재현하거나 킬 스위치로 쓴다. */
public class NoOpRateLimiter implements RateLimiter {

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        return true;
    }
}

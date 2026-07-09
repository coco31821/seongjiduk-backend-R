package com.sungjiduk.backend.common.ratelimit;

import java.time.Duration;
import java.util.Optional;

/** 제한 없음(항상 자리 있음). 가드 off 모드 — Redis 도입 "이전"의 무제한 동작을 재현하거나 킬 스위치로 쓴다. */
public class NoOpConcurrencyLimiter implements ConcurrencyLimiter {

    private static final Permit NO_OP = () -> { };

    @Override
    public Optional<Permit> acquire(String key, int maxConcurrent, Duration wait) {
        return Optional.of(NO_OP);
    }
}

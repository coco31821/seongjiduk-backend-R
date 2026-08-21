package com.sungjiduk.backend.common.ratelimit;

import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.trip.infra.AiBusyException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** 일정·설명·경로검증이 하나의 AI 서비스 용량을 공유하도록 하는 경계. */
@Component
public class AiConcurrencyGuard {
    private static final String GLOBAL_KEY = "ai:global";
    private final ConcurrencyLimiter limiter;
    private final RedisGuardProperties properties;
    private final MeterRegistry meterRegistry;
    public AiConcurrencyGuard(ConcurrencyLimiter limiter, RedisGuardProperties properties, MeterRegistry meterRegistry) {
        this.limiter = limiter;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }
    public <T> T execute(String operation, Supplier<T> action) {
        Optional<ConcurrencyLimiter.Permit> permit = limiter.acquire(GLOBAL_KEY, properties.getAiMaxConcurrent(), Duration.ofMillis(properties.getAiWaitMs()));
        if (permit.isEmpty()) {
            meterRegistry.counter("seongjiduk.guard.acquire", "guard", "ai", "operation", operation, "outcome", "timeout").increment();
            throw new AiBusyException("AI " + operation + " 동시성 한도 초과");
        }
        meterRegistry.counter("seongjiduk.guard.acquire", "guard", "ai", "operation", operation, "outcome", "granted").increment();
        try (ConcurrencyLimiter.Permit ignored = permit.get()) {
            T value = action.get();
            meterRegistry.counter("seongjiduk.ai.requests", "operation", operation, "outcome", "success").increment();
            return value;
        } catch (RuntimeException e) {
            meterRegistry.counter("seongjiduk.ai.requests", "operation", operation, "outcome", "error").increment();
            throw e;
        }
    }
}

package com.sungjiduk.backend.common.properties;

import lombok.Getter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Redis 기반 처리량/비용 가드 설정.
 * mode로 구현을 토글해 벤치마크 3열(무제한 → 인메모리 단일 → Redis 분산)을 만든다.
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "seongjiduk.redis-guard")
public class RedisGuardProperties {
    public enum Mode { OFF, LOCAL, REDIS }

    /** redis(분산) | local(인메모리 단일) | off(무제한, Redis 도입 전 재현·킬스위치) */
    @NotNull private final Mode cacheMode;
    @NotNull private final Mode rateLimitMode;
    @NotNull private final Mode concurrencyLimitMode;
    /** AI 생성(ai-service 호출) 동시 실행 상한. */
    @Min(1) private final int aiMaxConcurrent;
    /** 자리가 없을 때 큐에서 최대 대기(ms). 초과하면 로컬 폴백. */
    @Min(0) private final long aiWaitMs;
    /** 외부 API(네이버·구글) 키별 분당 호출 상한(공유 키 429·과금 방어). */
    @Min(1) private final int externalRpm;
    @Min(1000) private final long aiLeaseMs;
    @Min(1000) private final long aiHeartbeatMs;

    public RedisGuardProperties(
            @DefaultValue("REDIS") Mode cacheMode,
            @DefaultValue("REDIS") Mode rateLimitMode,
            @DefaultValue("REDIS") Mode concurrencyLimitMode,
            @DefaultValue("4") int aiMaxConcurrent,
            @DefaultValue("3000") long aiWaitMs,
            @DefaultValue("600") int externalRpm,
            @DefaultValue("120000") long aiLeaseMs,
            @DefaultValue("20000") long aiHeartbeatMs
    ) {
        this.cacheMode = cacheMode;
        this.rateLimitMode = rateLimitMode;
        this.concurrencyLimitMode = concurrencyLimitMode;
        this.aiMaxConcurrent = aiMaxConcurrent;
        this.aiWaitMs = aiWaitMs;
        this.externalRpm = externalRpm;
        this.aiLeaseMs = aiLeaseMs;
        this.aiHeartbeatMs = aiHeartbeatMs;
    }
}

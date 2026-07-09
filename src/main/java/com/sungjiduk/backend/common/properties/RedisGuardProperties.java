package com.sungjiduk.backend.common.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Redis 기반 처리량/비용 가드 설정.
 * mode로 구현을 토글해 벤치마크 3열(무제한 → 인메모리 단일 → Redis 분산)을 만든다.
 */
@Getter
@ConfigurationProperties(prefix = "seongjiduk.redis-guard")
public class RedisGuardProperties {

    /** redis(분산) | local(인메모리 단일) | off(무제한, Redis 도입 전 재현·킬스위치) */
    private final String mode;
    /** AI 생성(ai-service 호출) 동시 실행 상한. */
    private final int aiMaxConcurrent;
    /** 자리가 없을 때 큐에서 최대 대기(ms). 초과하면 로컬 폴백. */
    private final long aiWaitMs;
    /** 외부 API(네이버·구글) 키별 분당 호출 상한(공유 키 429·과금 방어). */
    private final int externalRpm;

    public RedisGuardProperties(
            @DefaultValue("redis") String mode,
            @DefaultValue("4") int aiMaxConcurrent,
            @DefaultValue("3000") long aiWaitMs,
            @DefaultValue("600") int externalRpm
    ) {
        this.mode = mode;
        this.aiMaxConcurrent = aiMaxConcurrent;
        this.aiWaitMs = aiWaitMs;
        this.externalRpm = externalRpm;
    }
}

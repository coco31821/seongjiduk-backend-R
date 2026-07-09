package com.sungjiduk.backend.common.ratelimit;

import java.time.Duration;
import java.util.Optional;

/**
 * 느리고 비싼 작업(AI 일정 생성 등)의 동시 실행 개수를 제한한다.
 * 인스턴스를 늘릴 수 없는 제약에서, 한 번에 처리하는 수를 묶어(세마포어) 다운스트림(ai-service) 과부하를 막고,
 * 초과 요청은 잠깐 줄을 세웠다가 자리가 안 나면 거절한다.
 *
 * <p>구현체는 인메모리(단일 인스턴스)와 Redis(분산 — 인스턴스 전체가 공유하는 동시 실행 한도)로 나뉜다.
 */
public interface ConcurrencyLimiter {

    /**
     * {@code key}에 대해 최대 {@code maxConcurrent}개까지 동시 진입을 허용한다.
     * 자리가 없으면 {@code wait}만큼 기다렸다가, 그래도 없으면 비어 있는 결과를 준다.
     *
     * @return 자리를 얻으면 해제용 {@link Permit}(try-with-resources로 반납), 못 얻으면 {@link Optional#empty()}
     */
    Optional<Permit> acquire(String key, int maxConcurrent, Duration wait);

    /** 획득한 자리를 반납하는 토큰. {@link #close()}는 한 번만 반납하도록 멱등해야 한다. */
    interface Permit extends AutoCloseable {
        @Override
        void close();
    }
}

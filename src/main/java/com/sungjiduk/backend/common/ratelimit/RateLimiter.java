package com.sungjiduk.backend.common.ratelimit;

import java.time.Duration;

/**
 * 외부 API 키(OpenAI·Google·Naver 등)를 부르기 직전에 호출 속도를 제어한다.
 * 공유 키가 429(레이트리밋)/과금 폭탄을 맞지 않도록 앞단에서 수도꼭지 역할을 한다.
 *
 * <p>고정 윈도우 카운터: {@code key}에 대해 {@code window} 구간 동안 {@code limit}회까지 허용한다.
 * 구현체는 인메모리(단일 인스턴스)와 Redis(분산 — 인스턴스를 넘어 공유 한도)로 나뉜다.
 */
public interface RateLimiter {

    /**
     * @param key    한도를 세는 단위(예: {@code "openai"}, {@code "google:places"})
     * @param limit  윈도우당 허용 횟수(&gt;0)
     * @param window 윈도우 길이
     * @return 허용되면 true, 한도 초과면 false
     */
    boolean tryAcquire(String key, int limit, Duration window);
}

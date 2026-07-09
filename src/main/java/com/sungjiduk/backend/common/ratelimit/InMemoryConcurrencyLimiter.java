package com.sungjiduk.backend.common.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 동시성 리미터의 인메모리 레퍼런스 구현(프로세스 로컬 {@link Semaphore} 기반).
 * Redis 구현({@link RedisConcurrencyLimiter})이 미러하는 기준 의미이자, Redis 비활성 시 폴백.
 *
 * <p>키별 세마포어는 최초 획득 시점의 {@code maxConcurrent}로 고정된다(설정값 고정 전제).
 */
public class InMemoryConcurrencyLimiter implements ConcurrencyLimiter {

    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    @Override
    public Optional<Permit> acquire(String key, int maxConcurrent, Duration wait) {
        if (maxConcurrent <= 0) {
            return Optional.empty();
        }
        Semaphore semaphore = semaphores.computeIfAbsent(key, k -> new Semaphore(maxConcurrent, true));
        try {
            if (semaphore.tryAcquire(Math.max(0, wait.toMillis()), TimeUnit.MILLISECONDS)) {
                return Optional.of(new SemaphorePermit(semaphore));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    /** 멱등 반납: close()를 여러 번 불러도 세마포어를 한 번만 release. */
    private static final class SemaphorePermit implements Permit {
        private final Semaphore semaphore;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private SemaphorePermit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}

package com.sungjiduk.backend.common.ratelimit;

import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter.Permit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * §7 처리량 가드 벤치마크(수동 실행 — CI 제외). 실행:
 * <pre>BENCHMARK=true ./gradlew test --tests "*RedisGuardBenchmark"</pre>
 * redis 모드는 localhost:6379 Redis가 떠 있어야 측정된다(없으면 스킵).
 *
 * <p>모델: ai-service는 동시 {@code AI_CAPACITY}개만 진짜로 처리하고 각 호출은 {@code AI_LATENCY_MS} 걸린다.
 * 그 이상 몰리면 {@code AI_QUEUE_MS} 안에 자리를 못 얻고 <b>과부하로 실패</b>(503처럼)한다.
 * 가드(off/local/redis)를 앞단에 두고 {@code CONCURRENCY}개를 동시에 쏴서
 * 성공률·처리량(req/s)·p95 지연을 비교한다.
 */
@DisplayName("RedisGuardBenchmark (수동)")
@EnabledIfEnvironmentVariable(named = "BENCHMARK", matches = "true")
class RedisGuardBenchmark {

    // 부하 파라미터
    private static final int CONCURRENCY = 40;      // 동시에 들어오는 요청 수
    private static final int REQUESTS_PER_THREAD = 5;
    private static final int GUARD_PERMITS = 2;     // 가드가 허용하는 동시 AI 호출
    private static final Duration GUARD_WAIT = Duration.ofMillis(3000);

    // ai-service 모사
    private static final int AI_CAPACITY = 2;       // ai-service가 진짜 동시에 처리 가능한 수
    private static final long AI_LATENCY_MS = 250;   // 1건 처리 시간(LLM 지연 모사)
    private static final long AI_QUEUE_MS = 100;     // 이보다 오래 못 기다리면 과부하 실패

    @Test
    @DisplayName("off / local / redis 처리량·성공률·p95 비교")
    void benchmark() throws Exception {
        System.out.println("\n===== §7 처리량 가드 벤치마크 =====");
        System.out.printf("부하: 동시 %d × %d회 = %d요청 | 가드 permits=%d wait=%dms%n",
                CONCURRENCY, REQUESTS_PER_THREAD, CONCURRENCY * REQUESTS_PER_THREAD, GUARD_PERMITS, GUARD_WAIT.toMillis());
        System.out.printf("모사 ai-service: 동시 %d · 지연 %dms · 큐한계 %dms%n%n", AI_CAPACITY, AI_LATENCY_MS, AI_QUEUE_MS);
        System.out.printf("%-8s | %7s | %7s | %7s | %10s | %8s | %8s%n",
                "mode", "총", "성공", "실패", "처리량/s", "평균ms", "p95ms");
        System.out.println("-".repeat(72));

        runArm("off", new NoOpConcurrencyLimiter());
        runArm("local", new InMemoryConcurrencyLimiter());
        StringRedisTemplate redis = tryRedis();
        if (redis != null) {
            runArm("redis", new RedisConcurrencyLimiter(redis));
        } else {
            System.out.printf("%-8s | (localhost:6379 Redis 없음 — 스킵. local과 동일 의미, 인스턴스 간 공유만 다름)%n", "redis");
        }
        System.out.println("=".repeat(72));
    }

    private void runArm(String mode, ConcurrencyLimiter guard) throws Exception {
        Semaphore aiCapacity = new Semaphore(AI_CAPACITY, true);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        java.util.concurrent.atomic.AtomicReference<String> firstError = new java.util.concurrent.atomic.AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY);
        long benchStart = System.nanoTime();

        for (int t = 0; t < CONCURRENCY; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int r = 0; r < REQUESTS_PER_THREAD; r++) {
                        long t0 = System.nanoTime();
                        boolean ok;
                        try {
                            ok = oneRequest(guard, aiCapacity);
                        } catch (RuntimeException ex) {
                            Throwable root = ex;
                            while (root.getCause() != null && root.getCause() != root) {
                                root = root.getCause();
                            }
                            firstError.compareAndSet(null, root.getClass().getSimpleName() + ": " + root.getMessage());
                            ok = false;
                        }
                        long ms = (System.nanoTime() - t0) / 1_000_000;
                        latencies.add(ms);
                        if (ok) success.incrementAndGet(); else failed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        long elapsedMs = (System.nanoTime() - benchStart) / 1_000_000;
        pool.shutdownNow();

        int total = success.get() + failed.get();
        double throughput = success.get() * 1000.0 / Math.max(1, elapsedMs);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = percentile(latencies, 95);
        System.out.printf("%-8s | %7d | %7d | %7d | %10.1f | %8.0f | %8d%n",
                mode, total, success.get(), failed.get(), throughput, avg, p95);
        if (firstError.get() != null) {
            System.out.printf("         └ 첫 예외: %s%n", firstError.get());
        }
    }

    /** 가드 획득 → 모사 ai 호출 → 반납. 가드 거절 또는 ai 과부하면 실패(false). */
    private boolean oneRequest(ConcurrencyLimiter guard, Semaphore aiCapacity) {
        Optional<Permit> permit = guard.acquire("ai:generate", GUARD_PERMITS, GUARD_WAIT);
        if (permit.isEmpty()) {
            return false; // 가드가 큐 대기 후에도 자리를 못 줌 → 폴백(성공으로 안 셈)
        }
        try (Permit p = permit.get()) {
            return callSimulatedAi(aiCapacity);
        }
    }

    private boolean callSimulatedAi(Semaphore aiCapacity) {
        try {
            if (!aiCapacity.tryAcquire(AI_QUEUE_MS, TimeUnit.MILLISECONDS)) {
                return false; // ai-service 과부하 → 실패(503)
            }
            try {
                Thread.sleep(AI_LATENCY_MS);
                return true;
            } finally {
                aiCapacity.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static long percentile(List<Long> values, int p) {
        if (values.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    private static StringRedisTemplate tryRedis() {
        try {
            String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
            int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
            LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
            factory.afterPropertiesSet();
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            template.hasKey("__ping__"); // 실제 연결 시도
            // 벤치용 로컬 Redis가 RDB 저장 실패로 쓰기를 막는 경우(MISCONF) 해제 — 운영 설정과 무관
            try {
                template.execute((org.springframework.data.redis.core.RedisCallback<Object>) conn -> {
                    conn.serverCommands().setConfig("stop-writes-on-bgsave-error", "no");
                    return null;
                });
            } catch (RuntimeException ignore) {
                // 권한 없거나 관리형 Redis면 무시(쓰기 정상이면 그대로 진행)
            }
            return template;
        } catch (RuntimeException e) {
            return null;
        }
    }
}

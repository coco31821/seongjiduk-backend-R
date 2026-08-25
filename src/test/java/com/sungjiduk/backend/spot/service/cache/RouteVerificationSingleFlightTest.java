package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter;
import com.sungjiduk.backend.common.ratelimit.InMemoryConcurrencyLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RouteVerificationSingleFlight")
class RouteVerificationSingleFlightTest {
    @Test
    @DisplayName("같은 contentId의 두 번째 요청은 소유자 종료 전 timeout 된다")
    void timesOutUntilOwnerReleases() throws Exception {
        RouteVerificationSingleFlight lock = new RouteVerificationSingleFlight(new InMemoryConcurrencyLimiter(), Duration.ofMillis(50));
        ConcurrencyLimiter.Permit owner = lock.acquire(1L).orElseThrow();
        try (var executor = Executors.newSingleThreadExecutor()) {
            assertThat(executor.submit(() -> lock.acquire(1L).isPresent()).get()).isFalse();
        } finally {
            owner.close();
        }
        assertThat(lock.acquire(1L)).isPresent();
    }

    @Test
    @DisplayName("contentId별 잠금은 독립적이다")
    void locksAreScopedToContent() {
        RouteVerificationSingleFlight lock = new RouteVerificationSingleFlight(new InMemoryConcurrencyLimiter(), Duration.ZERO);
        assertThat(lock.acquire(1L)).isPresent();
        assertThat(lock.acquire(2L)).isPresent();
    }
}

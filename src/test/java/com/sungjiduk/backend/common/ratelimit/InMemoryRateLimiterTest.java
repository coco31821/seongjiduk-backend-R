package com.sungjiduk.backend.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryRateLimiter")
class InMemoryRateLimiterTest {

    /** 테스트에서 시간을 임의로 전진시키기 위한 가변 Clock. */
    private static Clock mutableClock(AtomicReference<Instant> now) {
        return new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
            @Override public long millis() { return now.get().toEpochMilli(); }
        };
    }

    @Nested
    @DisplayName("tryAcquire는")
    class TryAcquire {

        @Test
        @DisplayName("윈도우당 limit회까지 허용하고 그 다음은 거부한다")
        void allowsUpToLimit() {
            // given — 1분 윈도우에 3회 한도
            var now = new AtomicReference<>(Instant.parse("2026-07-09T00:00:30Z"));
            RateLimiter limiter = new InMemoryRateLimiter(mutableClock(now));
            Duration window = Duration.ofMinutes(1);

            // when // then — 앞의 3회는 허용, 4회째는 거부
            assertThat(limiter.tryAcquire("openai", 3, window)).isTrue();
            assertThat(limiter.tryAcquire("openai", 3, window)).isTrue();
            assertThat(limiter.tryAcquire("openai", 3, window)).isTrue();
            assertThat(limiter.tryAcquire("openai", 3, window)).isFalse();
        }

        @Test
        @DisplayName("윈도우가 지나면 카운트가 리셋되어 다시 허용한다")
        void resetsAfterWindow() {
            // given — 한도를 소진한 상태
            var now = new AtomicReference<>(Instant.parse("2026-07-09T00:00:30Z"));
            RateLimiter limiter = new InMemoryRateLimiter(mutableClock(now));
            Duration window = Duration.ofMinutes(1);
            limiter.tryAcquire("openai", 2, window);
            limiter.tryAcquire("openai", 2, window);
            assertThat(limiter.tryAcquire("openai", 2, window)).isFalse();

            // when — 다음 윈도우로 시간 전진
            now.set(Instant.parse("2026-07-09T00:01:05Z"));

            // then — 다시 허용
            assertThat(limiter.tryAcquire("openai", 2, window)).isTrue();
        }

        @Test
        @DisplayName("키가 다르면 한도를 독립적으로 센다")
        void countsPerKey() {
            // given
            var now = new AtomicReference<>(Instant.parse("2026-07-09T00:00:30Z"));
            RateLimiter limiter = new InMemoryRateLimiter(mutableClock(now));
            Duration window = Duration.ofMinutes(1);

            // when — openai 한도 소진
            limiter.tryAcquire("openai", 1, window);
            assertThat(limiter.tryAcquire("openai", 1, window)).isFalse();

            // then — google은 영향 없음
            assertThat(limiter.tryAcquire("google", 1, window)).isTrue();
        }

        @Test
        @DisplayName("limit이 0 이하면 항상 거부한다")
        void deniesWhenLimitNotPositive() {
            // given
            var now = new AtomicReference<>(Instant.parse("2026-07-09T00:00:30Z"));
            RateLimiter limiter = new InMemoryRateLimiter(mutableClock(now));

            // when // then
            assertThat(limiter.tryAcquire("openai", 0, Duration.ofMinutes(1))).isFalse();
        }
    }
}

package com.sungjiduk.backend.common.ratelimit;

import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter.Permit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryConcurrencyLimiter")
class InMemoryConcurrencyLimiterTest {

    private static final Duration NO_WAIT = Duration.ZERO;

    @Nested
    @DisplayName("acquire는")
    class Acquire {

        @Test
        @DisplayName("maxConcurrent개까지 자리를 주고 초과 요청은 비운다")
        void grantsUpToMax() {
            // given — 동시 2개 한도
            ConcurrencyLimiter limiter = new InMemoryConcurrencyLimiter();

            // when — 2개 획득(반납 안 함)
            Optional<Permit> first = limiter.acquire("ai:generate", 2, NO_WAIT);
            Optional<Permit> second = limiter.acquire("ai:generate", 2, NO_WAIT);

            // then — 3번째는 자리 없음
            Optional<Permit> third = limiter.acquire("ai:generate", 2, NO_WAIT);
            assertThat(first).isPresent();
            assertThat(second).isPresent();
            assertThat(third).isEmpty();
        }

        @Test
        @DisplayName("자리를 반납하면 다음 요청이 다시 들어갈 수 있다")
        void releasesSlot() {
            // given — 1개 한도로 꽉 채움
            ConcurrencyLimiter limiter = new InMemoryConcurrencyLimiter();
            Permit held = limiter.acquire("ai:generate", 1, NO_WAIT).orElseThrow();
            assertThat(limiter.acquire("ai:generate", 1, NO_WAIT)).isEmpty();

            // when — 반납
            held.close();

            // then — 다시 획득 가능
            assertThat(limiter.acquire("ai:generate", 1, NO_WAIT)).isPresent();
        }

        @Test
        @DisplayName("close를 여러 번 불러도 자리를 한 번만 반납한다(멱등)")
        void closeIsIdempotent() {
            // given — 1개 한도, 하나 획득
            ConcurrencyLimiter limiter = new InMemoryConcurrencyLimiter();
            Permit held = limiter.acquire("ai:generate", 1, NO_WAIT).orElseThrow();

            // when — 같은 permit을 두 번 close
            held.close();
            held.close();

            // then — 자리는 1개만 남아, 하나 획득 후엔 다시 꽉 참
            assertThat(limiter.acquire("ai:generate", 1, NO_WAIT)).isPresent();
            assertThat(limiter.acquire("ai:generate", 1, NO_WAIT)).isEmpty();
        }

        @Test
        @DisplayName("키가 다르면 자리를 독립적으로 센다")
        void countsPerKey() {
            // given
            ConcurrencyLimiter limiter = new InMemoryConcurrencyLimiter();

            // when — describe 한도 소진
            limiter.acquire("ai:describe", 1, NO_WAIT);
            assertThat(limiter.acquire("ai:describe", 1, NO_WAIT)).isEmpty();

            // then — generate는 영향 없음
            assertThat(limiter.acquire("ai:generate", 1, NO_WAIT)).isPresent();
        }

        @Test
        @DisplayName("maxConcurrent가 0 이하면 자리를 주지 않는다")
        void deniesWhenMaxNotPositive() {
            // given
            ConcurrencyLimiter limiter = new InMemoryConcurrencyLimiter();

            // when // then
            assertThat(limiter.acquire("ai:generate", 0, NO_WAIT)).isEmpty();
        }
    }
}

package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("RedisRouteVerificationCache — 명시적 JSON 직렬화 왕복")
class RedisRouteVerificationCacheTest {

    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RouteVerificationResponse sample() {
        return new RouteVerificationResponse(
                1L, true, 12, 2,
                List.of(new RouteVerificationResponse.SpotMention(5L, 3,
                        List.of(new RouteVerificationResponse.Source("후기", "https://blog.naver.com/a/1", "20260701")))),
                List.of(new RouteVerificationResponse.VerifiedPair(5L, 6L, 2)),
                List.of(new RouteVerificationResponse.VerifiedCourse(1, List.of(5L, 6L), 2,
                        List.of(new RouteVerificationResponse.Source("후기", "https://blog.naver.com/a/1", "20260701")))),
                List.of());
    }

    @Test
    @DisplayName("put이 저장한 JSON을 get이 동일 객체로 복원한다 — 타입정보 없는 범용 직렬화의 무한 미스 재발 방지")
    void roundTrip() {
        // given — put이 쓴 문자열을 그대로 get이 읽도록 배선
        given(template.opsForValue()).willReturn(ops);
        final String[] stored = new String[1];
        org.mockito.Mockito.doAnswer(inv -> {
            stored[0] = inv.getArgument(1);
            return null;
        }).when(ops).set(anyString(), anyString(), any(Duration.class));
        given(ops.get(anyString())).willAnswer(inv -> stored[0]);

        RedisRouteVerificationCache cache = new RedisRouteVerificationCache(template, objectMapper);
        RouteVerificationResponse original = sample();

        // when
        cache.put(1L, original, Duration.ofHours(6));
        RouteVerificationResponse restored = cache.get(1L);

        // then — 복원 결과가 record equals로 동일 (Map으로 떨어지면 여기서 실패)
        assertThat(stored[0]).isNotNull();
        assertThat(restored).isEqualTo(original);
        assertThat(restored.courses().get(0).spotIds()).containsExactly(5L, 6L);
    }

    @Test
    @DisplayName("Redis 장애 시 fail-open — get은 null(재계산), put은 예외 없이 생략")
    void failOpen() {
        given(template.opsForValue()).willThrow(new org.springframework.data.redis.RedisConnectionFailureException("down"));
        RedisRouteVerificationCache cache = new RedisRouteVerificationCache(template, objectMapper);

        assertThat(cache.get(1L)).isNull();
        cache.put(1L, sample(), Duration.ofHours(6));   // 예외가 새면 테스트 실패
    }
}

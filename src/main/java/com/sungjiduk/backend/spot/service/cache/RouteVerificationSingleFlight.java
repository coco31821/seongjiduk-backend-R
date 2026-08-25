package com.sungjiduk.backend.spot.service.cache;

import com.sungjiduk.backend.common.ratelimit.ConcurrencyLimiter;
import java.time.Duration;
import java.util.Optional;

/** Limits a cache miss for one content to one computing request across replicas. */
public class RouteVerificationSingleFlight {
    private static final String KEY_PREFIX = "route-verification:single-flight:";
    private final ConcurrencyLimiter limiter;
    private final Duration wait;

    public RouteVerificationSingleFlight(ConcurrencyLimiter limiter, Duration wait) {
        this.limiter = limiter;
        this.wait = wait;
    }

    public Optional<ConcurrencyLimiter.Permit> acquire(Long contentId) {
        return limiter.acquire(KEY_PREFIX + contentId, 1, wait);
    }
}

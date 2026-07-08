package com.sungjiduk.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 시간 의존 로직(캐시 TTL 등)을 테스트에서 제어할 수 있게 Clock을 빈으로 노출한다. */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

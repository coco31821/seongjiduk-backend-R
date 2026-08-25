package com.sungjiduk.backend.common.config;

import com.sungjiduk.backend.common.properties.RedisGuardProperties;
import com.sungjiduk.backend.trip.service.cache.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class TripCacheConfig {
    @Bean
    StartLocationCache startLocationCache(RedisGuardProperties props, ObjectProvider<StringRedisTemplate> redis, ObjectMapper objectMapper) {
        return switch (props.getCacheMode()) {
            case OFF -> new NoOpStartLocationCache();
            case LOCAL -> new InMemoryStartLocationCache();
            case REDIS -> new RedisStartLocationCache(redis.getObject(), objectMapper);
        };
    }
}

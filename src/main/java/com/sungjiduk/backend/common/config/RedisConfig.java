package com.sungjiduk.backend.common.config;

import com.sungjiduk.backend.common.cache.CacheNames;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@EnableCaching
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);

        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith( // redis key는 문자열로 저장.
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(   // redis value는 json으로 저장. (JSON 직렬화)
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            CacheNames.NEARBY_ATTRACTIONS, defaultConfig.entryTtl(Duration.ofHours(6)),
            CacheNames.STREET_VIEW, defaultConfig.entryTtl(Duration.ofHours(6)),
            CacheNames.AI_DESCRIPTIONS, defaultConfig.entryTtl(Duration.ofDays(7)),
            CacheNames.ROUTE_VERIFICATION, defaultConfig.entryTtl(Duration.ofHours(6)),
            CacheNames.START_LOCATION, defaultConfig.entryTtl(Duration.ofDays(1))   // 출발지 지 오코딩
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }
}

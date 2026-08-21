package com.sungjiduk.backend.trip.service.cache;

import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

/** 명시적 JSON 역직렬화와 fail-open을 제공하는 Redis adapter. */
public class RedisStartLocationCache implements StartLocationCache {
    private static final Logger log = LoggerFactory.getLogger(RedisStartLocationCache.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    public RedisStartLocationCache(StringRedisTemplate redis, ObjectMapper objectMapper) { this.redis = redis; this.objectMapper = objectMapper; }
    @Override public Optional<ReverseGeocoder.LatLng> get(String address) {
        try {
            String json = redis.opsForValue().get(key(address));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, ReverseGeocoder.LatLng.class));
        } catch (Exception e) { log.warn("start-location cache read skipped: {}", e.getMessage()); return Optional.empty(); }
    }
    @Override public void put(String address, ReverseGeocoder.LatLng value, Duration ttl) {
        try { redis.opsForValue().set(key(address), objectMapper.writeValueAsString(value), ttl); }
        catch (Exception e) { log.warn("start-location cache write skipped: {}", e.getMessage()); }
    }
    private String key(String address) {
        String normalized = address.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        try { return "cache:v1:geocode:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}

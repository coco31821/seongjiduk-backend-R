package com.sungjiduk.backend.trip.service.cache;

import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStartLocationCache implements StartLocationCache {
    private final ConcurrentHashMap<String, ReverseGeocoder.LatLng> values = new ConcurrentHashMap<>();
    @Override public Optional<ReverseGeocoder.LatLng> get(String address) { return Optional.ofNullable(values.get(normalize(address))); }
    @Override public void put(String address, ReverseGeocoder.LatLng value, Duration ttl) { values.put(normalize(address), value); }
    private String normalize(String address) { return address.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT); }
}

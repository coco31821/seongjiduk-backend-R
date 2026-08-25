package com.sungjiduk.backend.trip.service.cache;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import java.time.Duration;
import java.util.Optional;
public class NoOpStartLocationCache implements StartLocationCache {
    public Optional<ReverseGeocoder.LatLng> get(String address) { return Optional.empty(); }
    public void put(String address, ReverseGeocoder.LatLng value, Duration ttl) { }
}

package com.sungjiduk.backend.trip.service.cache;

import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import java.time.Duration;
import java.util.Optional;

/** 출발지 지오코딩의 타입 안전 cache port. 주소 원문을 key로 저장하지 않는다. */
public interface StartLocationCache {
    Optional<ReverseGeocoder.LatLng> get(String address);
    void put(String address, ReverseGeocoder.LatLng value, Duration ttl);
}

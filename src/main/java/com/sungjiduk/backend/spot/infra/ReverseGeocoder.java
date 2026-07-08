package com.sungjiduk.backend.spot.infra;

import java.util.Optional;

/**
 * 좌표 → 주소 역지오코딩. 스위처블 인터페이스.
 * 실패하거나 키가 없으면 {@link Optional#empty()}를 반환하고, 서비스는 fallback(작품 city)으로 처리한다.
 */
public interface ReverseGeocoder {

    Optional<GeoResult> reverse(double lat, double lng);

    /** 주소/장소명 → 좌표 정지오코딩 — 출발지 앵커용. 실패·키 없음이면 empty. */
    Optional<LatLng> forward(String address);

    record GeoResult(String address, String city) {
    }

    record LatLng(double lat, double lng) {
    }
}

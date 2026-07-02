package com.sungjiduk.backend.trip.entity;

/**
 * TripStop이 가리키는 장소 종류.
 * PILGRIMAGE: 성지 스팟(pilgrimageSpotId 사용)
 * ATTRACTION: 주변 일반 관광지(nearbyAttractionId 사용)
 */
public enum SpotType {
    PILGRIMAGE,
    ATTRACTION
}

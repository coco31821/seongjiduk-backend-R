package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TripService.filterDominantCluster — 지리 실현성 가드")
class TripGeoClusterFilterTest {

    private final Content content = Content.create("니지가사키", "ANIME", "JP", "글로벌 성지 작품");

    private PilgrimageSpot spot(String name, double lat, double lng) {
        return PilgrimageSpot.create(content, name, "도시", BigDecimal.valueOf(lat), BigDecimal.valueOf(lng),
                "주소", 30, null);
    }

    private Map<Long, PilgrimageSpot> ordered(Object... idSpotPairs) {
        Map<Long, PilgrimageSpot> map = new LinkedHashMap<>();
        for (int i = 0; i < idSpotPairs.length; i += 2) {
            map.put((Long) idSpotPairs[i], (PilgrimageSpot) idSpotPairs[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("대륙 간 혼합(도쿄3+뉴욕2)이면 다수 클러스터(도쿄)만 남긴다")
    void keepsDominantClusterAcrossContinents() {
        Map<Long, PilgrimageSpot> spots = ordered(
                1L, spot("아키하바라", 35.70, 139.77),
                2L, spot("오다이바", 35.63, 139.78),
                3L, spot("칸다묘진", 35.70, 139.76),
                4L, spot("타임스스퀘어", 40.75, -73.98),
                5L, spot("센트럴파크", 40.78, -73.96));

        Map<Long, PilgrimageSpot> result = TripService.filterDominantCluster(spots, null);

        assertThat(result.keySet()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("출발지가 뉴욕이면 뉴욕 클러스터를 선택한다")
    void picksClusterNearestToStart() {
        Map<Long, PilgrimageSpot> spots = ordered(
                1L, spot("아키하바라", 35.70, 139.77),
                2L, spot("오다이바", 35.63, 139.78),
                3L, spot("칸다묘진", 35.70, 139.76),
                4L, spot("타임스스퀘어", 40.75, -73.98),
                5L, spot("센트럴파크", 40.78, -73.96));

        Map<Long, PilgrimageSpot> result = TripService.filterDominantCluster(
                spots, new ReverseGeocoder.LatLng(40.71, -74.00));   // 뉴욕 시청

        assertThat(result.keySet()).containsExactly(4L, 5L);
    }

    @Test
    @DisplayName("도쿄+오사카(약 400km)는 국내 이동 범위라 한 클러스터로 전부 유지한다")
    void keepsDomesticRange() {
        Map<Long, PilgrimageSpot> spots = ordered(
                1L, spot("아키하바라", 35.70, 139.77),
                2L, spot("도톤보리", 34.67, 135.50));

        Map<Long, PilgrimageSpot> result = TripService.filterDominantCluster(spots, null);

        assertThat(result.keySet()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("좌표 없는 스팟(미임포트 null 포함)은 판단하지 않고 통과시킨다")
    void passesThroughSpotsWithoutCoordinates() {
        Map<Long, PilgrimageSpot> spots = ordered(
                1L, spot("아키하바라", 35.70, 139.77),
                2L, spot("칸다묘진", 35.70, 139.76),
                3L, null,   // 미임포트
                4L, spot("타임스스퀘어", 40.75, -73.98));

        Map<Long, PilgrimageSpot> result = TripService.filterDominantCluster(spots, null);

        assertThat(result.keySet()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("스팟이 1개 이하이거나 전부 한 지역이면 그대로 반환한다")
    void returnsAsIsWhenNoSplit() {
        Map<Long, PilgrimageSpot> single = ordered(1L, spot("아키하바라", 35.70, 139.77));
        assertThat(TripService.filterDominantCluster(single, null)).isEqualTo(single);

        Map<Long, PilgrimageSpot> oneRegion = ordered(
                1L, spot("아키하바라", 35.70, 139.77),
                2L, spot("오다이바", 35.63, 139.78));
        assertThat(TripService.filterDominantCluster(oneRegion, null).keySet()).containsExactly(1L, 2L);
    }
}

package com.sungjiduk.backend.spot.repository;

import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.entity.SpotReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpotReferenceRepository extends JpaRepository<SpotReference, Long> {

    Optional<SpotReference> findBySpotAndSourceName(PilgrimageSpot spot, String sourceName);

    List<SpotReference> findBySpotInAndSourceName(Collection<PilgrimageSpot> spots, String sourceName);

    /**
     * 작품 목록의 장면 이미지(성지 id 오름차순). 각 작품의 첫 성지 이미지를 대표 썸네일로 쓰기 위해
     * 한 번의 쿼리로 모아 온다(작품별 개별 조회 N+1 회피). 결과는 [contentId, url] 행.
     */
    @Query("""
        select s.content.id, r.url
        from SpotReference r
        join r.spot s
        where s.content.id in :contentIds and r.sourceName = :sourceName
        order by s.id asc
    """)
    List<Object[]> findSceneImagesByContentIds(
            @Param("contentIds") Collection<Long> contentIds,
            @Param("sourceName") String sourceName);
}

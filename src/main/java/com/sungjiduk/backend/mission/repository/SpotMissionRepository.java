package com.sungjiduk.backend.mission.repository;

import com.sungjiduk.backend.mission.entity.SpotMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpotMissionRepository extends JpaRepository<SpotMission, Long> {

    boolean existsBySpotId(Long spotId);

    List<SpotMission> findBySpotIdOrderByIdAsc(Long spotId);

    List<SpotMission> findBySpot_Content_IdAndActiveTrueOrderBySpotIdAscIdAsc(Long contentId);
}

package com.sungjiduk.backend.mission.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.mission.constants.MissionOrigin;
import com.sungjiduk.backend.mission.constants.MissionType;
import com.sungjiduk.backend.mission.entity.SpotMission;
import com.sungjiduk.backend.mission.repository.SpotMissionRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult.AiMissionDraft;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MissionService {

    private final SpotMissionRepository missionRepository;
    private final PilgrimageSpotRepository spotRepository;

    public MissionService(SpotMissionRepository missionRepository, PilgrimageSpotRepository spotRepository) {
        this.missionRepository = missionRepository;
        this.spotRepository = spotRepository;
    }

    /** describe 훅에서 호출 — 읽기 전용 TX 밖에서 써야 하므로 REQUIRES_NEW. 스팟당 최초 1회만(멱등). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveDrafts(Long spotId, List<AiMissionDraft> drafts) {
        if (drafts == null || drafts.isEmpty() || missionRepository.existsBySpotId(spotId)) {
            return;
        }
        PilgrimageSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
        drafts.stream().limit(3).forEach(draft -> missionRepository.save(SpotMission.create(
                spot, draft.title(), draft.description(), parseType(draft.missionType()), MissionOrigin.AI)));
    }

    private MissionType parseType(String raw) {
        try {
            return MissionType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return MissionType.EXPERIENCE; // AI가 규격 밖 타입을 내면 체험으로 폴백
        }
    }
}

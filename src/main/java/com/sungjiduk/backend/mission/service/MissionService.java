package com.sungjiduk.backend.mission.service;

import com.sungjiduk.backend.admin.dto.request.AdminMissionUpdateRequest;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.mission.constants.MissionOrigin;
import com.sungjiduk.backend.mission.constants.MissionType;
import com.sungjiduk.backend.mission.dto.response.MissionResponse;
import com.sungjiduk.backend.mission.entity.SpotMission;
import com.sungjiduk.backend.mission.repository.SpotMissionRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult.AiMissionDraft;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.visit.dto.request.VisitCreateRequest;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import com.sungjiduk.backend.visit.service.VisitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MissionService {

    private final SpotMissionRepository missionRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final VisitService visitService;

    public MissionService(SpotMissionRepository missionRepository, PilgrimageSpotRepository spotRepository,
                          VisitService visitService) {
        this.missionRepository = missionRepository;
        this.spotRepository = spotRepository;
        this.visitService = visitService;
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

    @Transactional(readOnly = true)
    public List<MissionResponse> findByContent(Long contentId) {
        return missionRepository.findBySpot_Content_IdAndActiveTrueOrderBySpotIdAscIdAsc(contentId).stream()
                .map(m -> new MissionResponse(m.getId(), m.getSpot().getId(), m.getTitle(),
                        m.getDescription(), m.getMissionType().name()))
                .toList();
    }

    /** 미션 완료 → VisitService 재사용으로 방문기록 남김(여권 도장 연동). REQUIRES_NEW를 안 타므로 기본 TX면 충분. */
    @Transactional
    public VisitResponse complete(Long userId, Long missionId) {
        SpotMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));
        return visitService.create(userId, new VisitCreateRequest(
                mission.getSpot().getId(), "미션 완료: " + mission.getTitle(), null));
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> findBySpotForAdmin(Long spotId) {
        return missionRepository.findBySpotIdOrderByIdAsc(spotId).stream()
                .map(m -> new MissionResponse(m.getId(), m.getSpot().getId(), m.getTitle(),
                        m.getDescription(), m.getMissionType().name()))
                .toList();
    }

    @Transactional
    public MissionResponse updateByAdmin(Long missionId, AdminMissionUpdateRequest request) {
        SpotMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));
        mission.update(request.title(), request.description(),
                parseType(request.missionType()), request.active() == null || request.active());
        return new MissionResponse(mission.getId(), mission.getSpot().getId(), mission.getTitle(),
                mission.getDescription(), mission.getMissionType().name());
    }

    @Transactional
    public void deleteByAdmin(Long missionId) {
        if (!missionRepository.existsById(missionId)) {
            throw new BusinessException(ErrorCode.MISSION_NOT_FOUND);
        }
        missionRepository.deleteById(missionId);
    }

    private MissionType parseType(String raw) {
        try {
            return MissionType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return MissionType.EXPERIENCE; // AI가 규격 밖 타입을 내면 체험으로 폴백
        }
    }
}

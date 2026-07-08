package com.sungjiduk.backend.mission.service;

import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.mission.constants.MissionOrigin;
import com.sungjiduk.backend.mission.constants.MissionType;
import com.sungjiduk.backend.mission.entity.SpotMission;
import com.sungjiduk.backend.mission.repository.SpotMissionRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.AiDescribeClient;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult.AiMissionDraft;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("MissionService")
class MissionServiceTest {

    @Autowired
    private MissionService missionService;

    @Autowired
    private SpotMissionRepository spotMissionRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private AiDescribeClient aiDescribeClient;

    @Nested
    @DisplayName("saveDrafts는")
    class SaveDrafts {

        @Test
        @DisplayName("AI 초안을 스팟 미션으로 저장한다 — origin=AI, 미지 타입은 EXPERIENCE 폴백")
        void savesDraftsAsAiMissions() {
            // given — saveDrafts는 REQUIRES_NEW라 별도 TX에서 스팟을 봐야 하므로 셋업을 커밋한다
            Content content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "설명"));
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "神田明神", "東京都", new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                    "千代田区", 40, null));
            commitSetup();

            try {
                // when
                missionService.saveDrafts(spot.getId(), List.of(
                        new AiMissionDraft("에마 찾기", "뮤즈 그림 에마를 찾아보세요.", "FIND"),
                        new AiMissionDraft("이상한 타입", "폴백 확인.", "WEIRD")));

                // then
                List<SpotMission> saved = spotMissionRepository.findBySpotIdOrderByIdAsc(spot.getId());
                assertThat(saved).hasSize(2);
                assertThat(saved.get(0).getOrigin()).isEqualTo(MissionOrigin.AI);
                assertThat(saved.get(0).getMissionType()).isEqualTo(MissionType.FIND);
                assertThat(saved.get(1).getMissionType()).isEqualTo(MissionType.EXPERIENCE);
            } finally {
                cleanup(spot.getId(), content.getId());
            }
        }

        @Test
        @DisplayName("이미 미션이 있는 스팟이면 저장하지 않는다 (멱등)")
        void skipsWhenSpotAlreadyHasMissions() {
            // given — 커밋해야 REQUIRES_NEW 저장이 스팟을 본다
            Content content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "설명"));
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "神田明神", "東京都", new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                    "千代田区", 40, null));
            commitSetup();

            try {
                missionService.saveDrafts(spot.getId(), List.of(new AiMissionDraft("A", "a", "PHOTO")));

                // when
                missionService.saveDrafts(spot.getId(), List.of(new AiMissionDraft("B", "b", "PHOTO")));

                // then
                assertThat(spotMissionRepository.findBySpotIdOrderByIdAsc(spot.getId()))
                        .hasSize(1)
                        .extracting(SpotMission::getTitle).containsExactly("A");
            } finally {
                cleanup(spot.getId(), content.getId());
            }
        }
    }

    /** REQUIRES_NEW 저장이 셋업 데이터를 보도록 테스트 TX를 커밋한다. (ContentServiceTest와 동일 패턴) */
    private void commitSetup() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    /** 커밋된 데이터는 롤백으로 사라지지 않으므로 FK 순서(미션→스팟→작품)대로 직접 정리한다. */
    private void cleanup(Long spotId, Long contentId) {
        spotMissionRepository.deleteAll(spotMissionRepository.findBySpotIdOrderByIdAsc(spotId));
        spotRepository.deleteById(spotId);
        contentRepository.deleteById(contentId);
    }
}

package com.sungjiduk.backend.visit.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import com.sungjiduk.backend.visit.dto.request.VisitCreateRequest;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import com.sungjiduk.backend.visit.repository.VisitRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("VisitService")
class VisitServiceTest {

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRecordRepository visitRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    private User user;
    private PilgrimageSpot spot;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("fan@example.com").passwordHash("hash").nickname("muse_fan").build());
        Content content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "설명"));
        spot = spotRepository.save(PilgrimageSpot.create(
                content, "神田明神", "東京都千代田区",
                new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                "千代田区", 40, null));
    }

    @Nested
    @DisplayName("create는")
    class Create {

        @Test
        @DisplayName("방문 기록을 DB에 저장하고 스팟 이름과 함께 반환한다")
        void persistsVisitRecord() {
            // given
            VisitCreateRequest request = new VisitCreateRequest(spot.getId(), "첫 성지 인증", "https://img/1.jpg");

            // when
            VisitResponse response = visitService.create(user.getId(), request);

            // then
            assertThat(response.visitId()).isNotNull();
            assertThat(response.spotName()).isEqualTo("神田明神");
            var saved = visitRecordRepository.findById(response.visitId()).orElseThrow();
            assertThat(saved.getUser().getId()).isEqualTo(user.getId());
            assertThat(saved.getNote()).isEqualTo("첫 성지 인증");
            assertThat(saved.getVisitedAt()).isNotNull();
        }

        @Test
        @DisplayName("없는 스팟이면 SPOT_NOT_FOUND를 던진다")
        void throwsWhenSpotMissing() {
            // given
            VisitCreateRequest request = new VisitCreateRequest(99999L, null, null);

            // when / then
            assertThatThrownBy(() -> visitService.create(user.getId(), request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SPOT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findMyVisits는")
    class FindMyVisits {

        @Test
        @DisplayName("내 기록만 최근 방문 순으로 반환한다 — 다른 사용자 기록 제외")
        void returnsOnlyMyVisitsLatestFirst() {
            // given
            User other = userRepository.save(User.builder()
                    .email("other@example.com").passwordHash("hash").nickname("other").build());
            visitService.create(user.getId(), new VisitCreateRequest(spot.getId(), "첫번째", null));
            visitService.create(user.getId(), new VisitCreateRequest(spot.getId(), "두번째", null));
            visitService.create(other.getId(), new VisitCreateRequest(spot.getId(), "남의 기록", null));

            // when
            List<VisitResponse> visits = visitService.findMyVisits(user.getId());

            // then
            assertThat(visits).hasSize(2);
            assertThat(visits).extracting(VisitResponse::memo).doesNotContain("남의 기록");
        }
    }

    @Nested
    @DisplayName("delete는")
    class Delete {

        @Test
        @DisplayName("소유자면 삭제한다")
        void deletesWhenOwner() {
            // given
            VisitResponse created = visitService.create(user.getId(),
                    new VisitCreateRequest(spot.getId(), "지울 기록", null));

            // when
            visitService.delete(user.getId(), created.visitId());

            // then
            assertThat(visitRecordRepository.findById(created.visitId())).isEmpty();
        }

        @Test
        @DisplayName("소유자가 아니면 FORBIDDEN을 던지고 기록은 남는다")
        void forbidsWhenNotOwner() {
            // given
            User other = userRepository.save(User.builder()
                    .email("other@example.com").passwordHash("hash").nickname("other").build());
            VisitResponse created = visitService.create(other.getId(),
                    new VisitCreateRequest(spot.getId(), "남의 기록", null));

            // when / then
            assertThatThrownBy(() -> visitService.delete(user.getId(), created.visitId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
            assertThat(visitRecordRepository.findById(created.visitId())).isPresent();
        }

        @Test
        @DisplayName("없는 기록이면 VISIT_NOT_FOUND를 던진다")
        void throwsWhenVisitMissing() {
            // when / then
            assertThatThrownBy(() -> visitService.delete(user.getId(), 99999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.VISIT_NOT_FOUND);
        }
    }
}

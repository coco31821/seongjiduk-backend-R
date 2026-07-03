package com.sungjiduk.backend.admin.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@SpringBootTest
@DisplayName("AdminSpotService 클래스의")
public class AdminSpotServiceTest {
    @Autowired
    ContentRepository contentRepository;

    @Autowired
    PilgrimageSpotRepository pilgrimageSpotRepository;

    @Autowired
    AdminSpotService adminSpotService;

    @BeforeEach
    void create_content() {
        contentRepository.save(
            Content.builder()
                .id(1L)
                .title("러브라이브")
                .category("ANIME")
                .country("JAPAN")
                .description("니코니코니")
                .build()
        );
    }

    @Nested
    @DisplayName("create 매서드는")
    class test_for_create {
        @Test
        @DisplayName("올바른 입력이 들어오면 Response를 생성해야한다.")
        void create_Success() {
            // given
            Long contentId = 1L;

            AdminSpotUpsertRequest request = new AdminSpotUpsertRequest(
                contentId,
                "LH 기업지원허브",
                "성남시",
                "대왕판교로 815",
                123.456,
                986.543,
                60,
                "http://reference.url"
            );

            // when
            AdminCommandResponse response = adminSpotService.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("CREATED");

        }

        @Test
        @DisplayName("올바르지 않은 작품이 들어오면 실패해야 한다.")
        void create_fail() {
            Long contentId = 2L;

            AdminSpotUpsertRequest request = new AdminSpotUpsertRequest(
                contentId,
                "LH 기업지원허브",
                "성남시",
                "대왕판교로 815",
                123.456,
                986.543,
                60,
                "http://reference.url"
            );

            assertThatThrownBy(() -> adminSpotService.create(request)).isInstanceOf(RuntimeException.class);
        }
    }
}

package com.sungjiduk.backend.admin.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.exception.ContentNotFoundException;
import com.sungjiduk.backend.common.config.SecurityConfig;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@DisplayName("AdminSpotService")
public class AdminSpotServiceTest {
    @Autowired
    ContentRepository contentRepository;

    @Autowired
    AdminSpotService adminSpotService;

    @BeforeEach
    void create_content() {
        contentRepository.save(
            Content.builder()
                .title("러브라이브")
                .category("ANIME")
                .country("JAPAN")
                .description("니코니코니")
                .build()
        );
    }

    @Nested
    @DisplayName("create는")
    class create {
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
                new BigDecimal("123.456"),
                new BigDecimal("123.456"),
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
            Long contentId = 9999L;

            AdminSpotUpsertRequest request = new AdminSpotUpsertRequest(
                contentId,
                "LH 기업지원허브",
                "성남시",
                "대왕판교로 815",
                new BigDecimal("123.456"),
                new BigDecimal("123.456"),
                60,
                "http://reference.url"
            );

            assertThatThrownBy(() -> adminSpotService.create(request)).isInstanceOf(ContentNotFoundException.class);
        }
    }
}

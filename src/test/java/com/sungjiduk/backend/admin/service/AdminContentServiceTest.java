package com.sungjiduk.backend.admin.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sungjiduk.backend.admin.dto.request.AdminContentUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@DisplayName("AdminContentService")
public class AdminContentServiceTest {
    @Autowired
    AdminContentService adminContentService;

    @Nested
    @DisplayName("create는")
    class create {
        @Test
        @DisplayName("올바른 입력이 들어오면 작품 등록에 성공해야한다.")
        void create_Success() {
            // given

            AdminContentUpsertRequest request = new AdminContentUpsertRequest(
                "러브라이브!",
                "ANIME",
                "JAPAN",
                "폐교위기에 놓인 학교를 살리기 위해 죽음의 데스매치 대회를 개최합니다."
            );

            // when
            AdminCommandResponse response = adminContentService.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("CREATED");
        }
    }
}

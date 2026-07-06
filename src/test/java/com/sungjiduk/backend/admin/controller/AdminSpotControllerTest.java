package com.sungjiduk.backend.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.exception.ContentNotFoundException;
import com.sungjiduk.backend.admin.service.AdminContentService;
import com.sungjiduk.backend.admin.service.AdminSpotService;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.user.service.UserService;

@DisplayName("AdminSpotController")
@WebMvcTest(AdminSpotController.class)
class AdminSpotControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSpotService adminSpotService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AdminContentService adminContentService;

    @Nested
    @DisplayName("create는")
    class create {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("정상적인 성지 스팟이 주어지면 HTTP OK가 되어야한다.")
        void create_success() throws Exception {
            // given
            String jsonRequest = """
                {
                    "contentId": 1,
                    "name": "LH 기업지원허브",
                    "city": "성남시",
                    "address": "대왕판교로 815",
                    "lat": 123.456,
                    "lng": 986.543,
                    "recommendedDurationMin": 60,
                    "referenceUrl": "http://reference.url"
                }
                """;

            given(adminSpotService.create(any(AdminSpotUpsertRequest.class)))
                .willReturn(new AdminCommandResponse(1L, "CREATED"));

            //when
            mockMvc.perform(post("/api/admin/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                    .value("CREATED"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("권한이 부족하면 HTTP 401이 되어야한다.")
        void create_failed_no_auth() throws Exception {
            // given
            String jsonRequest = """
                {
                    "contentId": 1,
                    "name": "LH 기업지원허브",
                    "city": "성남시",
                    "address": "대왕판교로 815",
                    "lat": 123.456,
                    "lng": 986.543,
                    "recommendedDurationMin": 60,
                    "referenceUrl": "http://reference.url"
                }
                """;

            given(adminSpotService.create(any(AdminSpotUpsertRequest.class)))
                .willReturn(new AdminCommandResponse(1L, "CREATED"));

            //when
            mockMvc.perform(post("/api/admin/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("로그인 하지 않은채로 접근 시 HTTP 401이 되어야한다.")
        void create_failed_no_login() throws Exception {
            // given
            String jsonRequest = """
                {
                    "contentId": 1,
                    "name": "LH 기업지원허브",
                    "city": "성남시",
                    "address": "대왕판교로 815",
                    "lat": 123.456,
                    "lng": 986.543,
                    "recommendedDurationMin": 60,
                    "referenceUrl": "http://reference.url"
                }
                """;

            given(adminSpotService.create(any(AdminSpotUpsertRequest.class)))
                .willReturn(new AdminCommandResponse(1L, "CREATED"));

            //when
            mockMvc.perform(post("/api/admin/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("작품을 찾을 수 없어서 등록에 실패하면 HTTP 404가 되어야 한다.")
        void create_failed_bad_request() throws Exception {
            // given
            String jsonRequest = """
                {
                    "contentId": 999,
                    "name": "LH 기업지원허브",
                    "city": "성남시",
                    "address": "대왕판교로 815",
                    "lat": 123.456,
                    "lng": 986.543,
                    "recommendedDurationMin": 60,
                    "referenceUrl": "http://reference.url"
                }
                """;

            given(adminSpotService.create(any(AdminSpotUpsertRequest.class)))
                .willThrow(new ContentNotFoundException(999L));

            //when
            mockMvc.perform(post("/api/admin/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isNotFound());
        }
    }
}

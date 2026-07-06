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

import com.sungjiduk.backend.admin.dto.request.AdminContentUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.service.AdminContentService;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.user.service.UserService;

@DisplayName("AdminContentController")
@WebMvcTest(AdminContentController.class)
class AdminContentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AdminContentService adminContentService;

    String contentJson = """
    {
        "title": "러브라이브!",
        "category": "ANIME",
        "country": "JAPAN",
        "description": "폐교위기에 놓인 학교를 살리기 위해 죽음의 데스매치 대회를 개최합니다."
    }
    """;

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("create는")
    class create {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("정상적인 작품이 주어지면 HTTP OK가 되어야한다.")
        void create_success() throws Exception {
            // given
            given(adminContentService.create(any(AdminContentUpsertRequest.class)))
                .willReturn(new AdminCommandResponse(1L, "CREATED"));

            //when
            mockMvc.perform(post("/api/admin/contents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contentJson)
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
            given(adminContentService.create(any(AdminContentUpsertRequest.class)))
                .willReturn(new AdminCommandResponse(1L, "CREATED"));

            //when
            mockMvc.perform(post("/api/admin/contents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contentJson)
                    .with(csrf()))
                // then
                .andExpect((status().isUnauthorized()));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("로그인이 되지 않았으면 HTTP 401이 되어야한다.")
        void create_failed_no_login() throws Exception {
            // given
            given(adminContentService.create(any(AdminContentUpsertRequest.class)))
                .willReturn(new AdminCommandResponse(1L, "CREATED"));

            //when
            mockMvc.perform(post("/api/admin/contents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contentJson)
                    .with(csrf()))
                // then
                .andExpect((status().isUnauthorized()));
        }
    }


}

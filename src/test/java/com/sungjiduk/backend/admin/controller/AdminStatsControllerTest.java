package com.sungjiduk.backend.admin.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.admin.service.AdminStatsService;
import com.sungjiduk.backend.common.config.SecurityConfig;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.user.service.UserService;

@Import(SecurityConfig.class)
@WebMvcTest(AdminStatsController.class)
@DisplayName("AdminStatsController")
public class AdminStatsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    AdminStatsService adminStatsService;

    @Nested
    @DisplayName("overview는")
    class overview {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("유저 권한이 ADMIN이면 HTTP OK를 반환해야 한다.")
        void overview_success() throws Exception {
            // given
            given(adminStatsService.overview()).willReturn(
                new AdminStatsOverviewResponse(
                1,
                1,
                1,
                "꽃보다 남자",
                "신화고등학교")
            );

            // when
            mockMvc.perform(get("/api/admin/stats/overview"))
                // then
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("유저 권한이 부족하면 HTTP 403을 반환해야 한다.")
        void overview_fail_no_auth() throws Exception {
            // given
            given(adminStatsService.overview()).willReturn(
                new AdminStatsOverviewResponse(
                    1,
                    1,
                    1,
                    "꽃보다 남자",
                    "신화고등학교")
            );

            // when
            mockMvc.perform(get("/api/admin/stats/overview"))
                // then
                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("로그인이 되어 있지 않으면 HTTP 403을 반환해야 한다.")
        void overview_fail_no_login() throws Exception {
            // given
            given(adminStatsService.overview()).willReturn(
                new AdminStatsOverviewResponse(
                    1,
                    1,
                    1,
                    "꽃보다 남자",
                    "신화고등학교")
            );

            // when
            mockMvc.perform(get("/api/admin/stats/overview"))
                // then
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("visitors는")
    class visitors {
        // given
        @BeforeEach
        void given_visitors() {
            given(adminStatsService.visitors()).willReturn(
                new StatsSeriesResponse(
                    "서비스 이용 통계",
                    List.of(
                        new StatsSeriesResponse.Point("오늘 방문 수", "1"),
                        new StatsSeriesResponse.Point("회원가입 수", "1"),
                        new StatsSeriesResponse.Point("로그인 수", "1"),
                        new StatsSeriesResponse.Point("회원 이용 수", "1"),
                        new StatsSeriesResponse.Point("비회원 이용 수", "1")
                    )
                )
            );
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("유저 권한이 ADMIN이면 HTTP OK를 반환해야 한다.")
        void success() throws Exception {
            // when
            mockMvc.perform(get("/api/admin/stats/visitors"))
                // then
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("유저 권한이 USER이면 HTTP 403을 반환해야 한다.")
        void failed_no_auth() throws Exception {
            // when
            mockMvc.perform(get("/api/admin/stats/visitors"))
                // then
                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("로그인이 되어 있지 않다면 HTTP OK를 반환해야 한다.")
        void failed_no_login() throws Exception {
            // when
            mockMvc.perform(get("/api/admin/stats/visitors"))
                // then
                .andExpect(status().isForbidden());
        }

    }

    @Nested
    @DisplayName("usage는")
    class usage {
        // given
        @BeforeEach
        void given_usage() {
            given(adminStatsService.visitors()).willReturn(
                new StatsSeriesResponse("서비스 이용 통계", List.of(
                    new StatsSeriesResponse.Point("총 일정 생성 수", "1"),
                    new StatsSeriesResponse.Point("오늘 일정 생성 수", "1"),
                    new StatsSeriesResponse.Point("오늘 AI 생성 수","1"),
                    new StatsSeriesResponse.Point("오늘 AI  재생성 수", "1"),
                    new StatsSeriesResponse.Point("인기 작품", "스타크래프트"),
                    new StatsSeriesResponse.Point("인기 성지", "코랄 수도성"),
                    new StatsSeriesResponse.Point("저장 수", "1"),
                    new StatsSeriesResponse.Point("공유 수", "1"))
                )
            );
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("유저 권한이 ADMIN이면 HTTP OK를 반환해야 한다.")
        void success() throws Exception {
            // when
            mockMvc.perform(get("/api/admin/stats/usage"))
                // then
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("유저 권한이 USER이면 HTTP 403을 반환해야 한다.")
        void failed_no_auth() throws Exception {
            // when
            mockMvc.perform(get("/api/admin/stats/usage"))
                // then
                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("로그인이 되어 있지 않다면 HTTP OK를 반환해야 한다.")
        void failed_no_login() throws Exception {
            // when
            mockMvc.perform(get("/api/admin/stats/usage"))
                // then
                .andExpect(status().isForbidden());
        }
    }
}

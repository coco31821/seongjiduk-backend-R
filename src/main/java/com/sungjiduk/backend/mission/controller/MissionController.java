package com.sungjiduk.backend.mission.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.mission.dto.response.MissionResponse;
import com.sungjiduk.backend.mission.service.MissionService;
import com.sungjiduk.backend.user.entity.CurrentUser;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping("/api/contents/{contentId}/missions")
    public ApiResponse<List<MissionResponse>> byContent(@PathVariable Long contentId) {
        return ApiResponse.ok(missionService.findByContent(contentId));
    }

    @PostMapping("/api/missions/{missionId}/complete")
    public ApiResponse<VisitResponse> complete(@AuthenticationPrincipal CurrentUser currentUser,
                                               @PathVariable Long missionId) {
        return ApiResponse.ok(missionService.complete(currentUser.getId(), missionId));
    }
}

package com.sungjiduk.backend.mission.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.mission.dto.response.MissionResponse;
import com.sungjiduk.backend.mission.service.MissionService;
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
}

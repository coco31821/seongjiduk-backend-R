package com.sungjiduk.backend.admin.controller;

import com.sungjiduk.backend.admin.dto.request.AdminMissionUpdateRequest;
import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.mission.dto.response.MissionResponse;
import com.sungjiduk.backend.mission.service.MissionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminMissionController {

    private final MissionService missionService;

    public AdminMissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping("/api/admin/spots/{spotId}/missions")
    public ApiResponse<List<MissionResponse>> bySpot(@PathVariable Long spotId) {
        return ApiResponse.ok(missionService.findBySpotForAdmin(spotId));
    }

    @PatchMapping("/api/admin/missions/{missionId}")
    public ApiResponse<MissionResponse> update(@PathVariable Long missionId,
                                               @RequestBody AdminMissionUpdateRequest request) {
        return ApiResponse.ok(missionService.updateByAdmin(missionId, request));
    }

    @DeleteMapping("/api/admin/missions/{missionId}")
    public ApiResponse<Void> delete(@PathVariable Long missionId) {
        missionService.deleteByAdmin(missionId);
        return ApiResponse.ok();
    }
}

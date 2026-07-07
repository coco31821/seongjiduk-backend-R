package com.sungjiduk.backend.visit.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.user.entity.CurrentUser;
import com.sungjiduk.backend.visit.dto.request.VisitCreateRequest;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import com.sungjiduk.backend.visit.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping
    public ApiResponse<VisitResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                             @Valid @RequestBody VisitCreateRequest request) {
        return ApiResponse.ok(visitService.create(currentUser.getId(), request));
    }

    @GetMapping("/me")
    public ApiResponse<List<VisitResponse>> myVisits(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(visitService.findMyVisits(currentUser.getId()));
    }

    @DeleteMapping("/{visitId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser currentUser,
                                    @PathVariable Long visitId) {
        visitService.delete(currentUser.getId(), visitId);
        return ApiResponse.ok();
    }
}

package com.sungjiduk.backend.visit.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.visit.dto.request.VisitCreateRequest;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import com.sungjiduk.backend.visit.service.VisitService;
import jakarta.validation.Valid;
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
    public ApiResponse<VisitResponse> create(@Valid @RequestBody VisitCreateRequest request) {
        return ApiResponse.ok(visitService.create(request));
    }

    @GetMapping("/me")
    public ApiResponse<List<VisitResponse>> myVisits() {
        return ApiResponse.ok(visitService.findMyVisits());
    }

    @DeleteMapping("/{visitId}")
    public ApiResponse<Void> delete(@PathVariable Long visitId) {
        visitService.delete(visitId);
        return ApiResponse.ok();
    }
}

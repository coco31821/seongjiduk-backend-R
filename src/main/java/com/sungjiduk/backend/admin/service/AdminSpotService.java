package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.request.SpotReportProcessRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.dto.response.SpotReportAdminResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminSpotService {

    public AdminCommandResponse create(AdminSpotUpsertRequest request) {
        return new AdminCommandResponse(1L, "CREATED");
    }

    public AdminCommandResponse update(Long spotId, AdminSpotUpsertRequest request) {
        return new AdminCommandResponse(spotId, "UPDATED");
    }

    public AdminCommandResponse delete(Long spotId) {
        return new AdminCommandResponse(spotId, "DELETED");
    }

    public List<SpotReportAdminResponse> findReports() {
        return List.of(new SpotReportAdminResponse(1L, "쇼헤이바시", "Tokyo, Japan", "PENDING"));
    }

    public AdminCommandResponse processReport(Long reportId, SpotReportProcessRequest request) {
        return new AdminCommandResponse(reportId, request.status());
    }
}

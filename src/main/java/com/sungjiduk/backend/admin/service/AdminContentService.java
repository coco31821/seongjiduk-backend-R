package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.request.AdminContentUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import org.springframework.stereotype.Service;

@Service
public class AdminContentService {

    public AdminCommandResponse create(AdminContentUpsertRequest request) {
        return new AdminCommandResponse(1L, "CREATED");
    }

    public AdminCommandResponse update(Long contentId, AdminContentUpsertRequest request) {
        return new AdminCommandResponse(contentId, "UPDATED");
    }

    public AdminCommandResponse delete(Long contentId) {
        return new AdminCommandResponse(contentId, "DELETED");
    }
}

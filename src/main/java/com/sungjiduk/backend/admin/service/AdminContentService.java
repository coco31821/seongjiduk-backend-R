package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.request.AdminContentUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminContentService {
    private final ContentRepository contentRepository;

    @Transactional
    public AdminCommandResponse create(AdminContentUpsertRequest request) {
        Content content = contentRepository.save(
            Content.builder()
                .title(request.title())
                .category(request.category())
                .country(request.country())
                .description(request.description())
                .build()
            );

        return new AdminCommandResponse(content.getId(), "CREATED");
    }

    public AdminCommandResponse update(Long contentId, AdminContentUpsertRequest request) {
        return new AdminCommandResponse(contentId, "UPDATED");
    }

    public AdminCommandResponse delete(Long contentId) {
        return new AdminCommandResponse(contentId, "DELETED");
    }
}

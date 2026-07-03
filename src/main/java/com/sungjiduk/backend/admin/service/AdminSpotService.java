package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.request.SpotReportProcessRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.dto.response.SpotReportAdminResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminSpotService {
    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository pilgrimageSpotRepository;

    @Transactional
    public AdminCommandResponse create(AdminSpotUpsertRequest request) {
        Content content = findContent(request.contentId());

        if (content == null) {
            throw new RuntimeException("해당 작품을 찾을 수 없습니다.");
        }

        PilgrimageSpot save = pilgrimageSpotRepository.save(
            PilgrimageSpot.builder()
            .content(content)
            .name(request.name())
            .address(request.address())
            .lat(request.lat())
            .lng(request.lng())
            .city(request.city())
            .recommendedDurationMin(request.recommendedDurationMin())
            .referenceUrl(request.referenceUrl())
            .build()
        );

        return new AdminCommandResponse(save.getId(), "CREATED");
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

    public Content findContent(Long id) {
        Optional<Content> byId = contentRepository.findById(id);

        return byId.orElse(null);
    }
}

package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.spot.dto.request.SpotReportCreateRequest;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
import com.sungjiduk.backend.spot.dto.response.SpotReportResponse;
import org.springframework.stereotype.Service;

@Service
public class SpotService {

    public SpotDetailResponse findSpot(Long spotId) {
        return new SpotDetailResponse(spotId, "쇼헤이바시", "Tokyo", "Tokyo, Japan", 35.697, 139.771, 30, "https://example.com/reference");
    }

    public SpotReportResponse createReport(SpotReportCreateRequest request) {
        return new SpotReportResponse(1L, "PENDING");
    }
}

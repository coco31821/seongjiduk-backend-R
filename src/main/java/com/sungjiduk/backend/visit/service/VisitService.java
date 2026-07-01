package com.sungjiduk.backend.visit.service;

import com.sungjiduk.backend.visit.dto.request.VisitCreateRequest;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitService {

    public VisitResponse create(VisitCreateRequest request) {
        return new VisitResponse(1L, request.spotId(), "쇼헤이바시", request.memo(), request.photoUrl());
    }

    public List<VisitResponse> findMyVisits() {
        return List.of(new VisitResponse(1L, 1L, "쇼헤이바시", "첫 성지 인증", "https://example.com/photo.jpg"));
    }

    public void delete(Long visitId) {
    }
}

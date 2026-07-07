package com.sungjiduk.backend.visit.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import com.sungjiduk.backend.visit.dto.request.VisitCreateRequest;
import com.sungjiduk.backend.visit.dto.response.VisitResponse;
import com.sungjiduk.backend.visit.entity.VisitRecord;
import com.sungjiduk.backend.visit.repository.VisitRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VisitService {

    private final VisitRecordRepository visitRecordRepository;
    private final UserRepository userRepository;
    private final PilgrimageSpotRepository spotRepository;

    public VisitService(VisitRecordRepository visitRecordRepository,
                        UserRepository userRepository,
                        PilgrimageSpotRepository spotRepository) {
        this.visitRecordRepository = visitRecordRepository;
        this.userRepository = userRepository;
        this.spotRepository = spotRepository;
    }

    @Transactional
    public VisitResponse create(Long userId, VisitCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        PilgrimageSpot spot = spotRepository.findById(request.spotId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));

        VisitRecord saved = visitRecordRepository.save(VisitRecord.builder()
                .user(user)
                .spot(spot)
                .note(request.memo())
                .imageUrl(request.photoUrl())
                .build());
        return toResponse(saved);
    }

    public List<VisitResponse> findMyVisits(Long userId) {
        return visitRecordRepository.findByUserIdOrderByVisitedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long visitId) {
        VisitRecord visit = visitRecordRepository.findById(visitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
        if (!visit.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        visitRecordRepository.delete(visit);
    }

    private VisitResponse toResponse(VisitRecord visit) {
        return new VisitResponse(
                visit.getId(),
                visit.getSpot().getId(),
                visit.getSpot().getName(),
                visit.getNote(),
                visit.getImageUrl());
    }
}

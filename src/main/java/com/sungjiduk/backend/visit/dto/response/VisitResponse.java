package com.sungjiduk.backend.visit.dto.response;

public record VisitResponse(
        Long visitId,
        Long spotId,
        String spotName,
        Long contentId,      // 성지 여권: 방문을 작품별로 묶는 매핑
        String contentTitle,
        String memo,
        String photoUrl
) {
}

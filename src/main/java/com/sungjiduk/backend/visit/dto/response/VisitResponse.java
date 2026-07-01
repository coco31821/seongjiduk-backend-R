package com.sungjiduk.backend.visit.dto.response;

public record VisitResponse(
        Long visitId,
        Long spotId,
        String spotName,
        String memo,
        String photoUrl
) {
}

package com.sungjiduk.backend.spot.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Anitabi {@code /bangumi/{id}/lite} 응답에서 필요한 필드만.
 * city는 지오코딩 실패 시 fallback 값으로 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnitabiWork(
        String title,
        String city
) {
}

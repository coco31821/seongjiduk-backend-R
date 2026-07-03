package com.sungjiduk.backend.event.dto.request;

import com.sungjiduk.backend.event.entity.EventType;
import jakarta.validation.constraints.NotNull;

/**
 * 접속/행동 이벤트 수집 요청 (POST /api/events).
 * userId, occurredAt은 서버가 채우므로 요청에 포함하지 않는다.
 */
public record EventCreateRequest(
        @NotNull EventType eventType,
        String path,
        Long targetId,
        String sessionId
) {
}

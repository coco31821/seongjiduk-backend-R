package com.sungjiduk.backend.event.service;

import com.sungjiduk.backend.event.dto.request.EventCreateRequest;
import com.sungjiduk.backend.event.entity.UsageEvent;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final UsageEventRepository usageEventRepository;

    public EventService(UsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    /** 접속/행동 이벤트 1건을 적재한다. userId는 인증 사용자면 채우고, 비회원이면 null. */
    @Transactional
    public void record(EventCreateRequest request, Long userId) {
        usageEventRepository.save(UsageEvent.builder()
                .userId(userId)
                .eventType(request.eventType())
                .path(request.path())
                .targetId(request.targetId())
                .sessionId(request.sessionId())
                .build());
    }
}

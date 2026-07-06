package com.sungjiduk.backend.event.controller;

import com.sungjiduk.backend.event.dto.request.EventCreateRequest;
import com.sungjiduk.backend.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 접속/행동 이벤트 수집 (EVENT-001). 프론트가 PAGE_VIEW 등을 전송.
     * userId는 인증 연동 후 SecurityContext에서 채운다(현재는 비회원 null).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void collect(@Valid @RequestBody EventCreateRequest request) {
        eventService.record(request, null);
    }
}

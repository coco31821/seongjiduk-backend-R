package com.sungjiduk.backend.event.service;

import com.sungjiduk.backend.event.dto.request.EventCreateRequest;
import com.sungjiduk.backend.event.entity.EventType;
import com.sungjiduk.backend.event.entity.UsageEvent;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("EventService")
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Nested
    @DisplayName("record는")
    class Record {

        @Test
        @DisplayName("이벤트를 UsageEvent로 저장한다")
        void savesUsageEvent() {
            // given
            EventCreateRequest request = new EventCreateRequest(EventType.PAGE_VIEW, "/map", 1L, "anon-1");

            // when
            eventService.record(request, 10L);

            // then
            UsageEvent saved = usageEventRepository.findAll().get(0);
            assertThat(saved.getEventType()).isEqualTo(EventType.PAGE_VIEW);
            assertThat(saved.getPath()).isEqualTo("/map");
            assertThat(saved.getTargetId()).isEqualTo(1L);
            assertThat(saved.getSessionId()).isEqualTo("anon-1");
            assertThat(saved.getUserId()).isEqualTo(10L);
            assertThat(saved.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("비회원(userId가 null)도 저장한다")
        void savesAnonymousEvent() {
            // given
            EventCreateRequest request = new EventCreateRequest(EventType.PAGE_VIEW, "/", null, "anon-2");

            // when
            eventService.record(request, null);

            // then
            UsageEvent saved = usageEventRepository.findAll().get(0);
            assertThat(saved.getUserId()).isNull();
            assertThat(saved.getEventType()).isEqualTo(EventType.PAGE_VIEW);
        }
    }
}

package com.sungjiduk.backend.event.repository;

import java.time.LocalDateTime;

import com.sungjiduk.backend.event.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
    Long countUsageEventByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
}

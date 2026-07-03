package com.sungjiduk.backend.event.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sungjiduk.backend.event.entity.UsageEvent;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
    long countUsageEventByCreatedAt(LocalDateTime createdAt);

    long countUsageEventByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}

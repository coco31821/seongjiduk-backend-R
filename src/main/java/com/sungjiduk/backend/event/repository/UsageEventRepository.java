package com.sungjiduk.backend.event.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.event.entity.EventType;
import com.sungjiduk.backend.event.entity.UsageEvent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
    @Query(
        """
        SELECT s.content
        FROM TripPlan s
        WHERE s.createdAt BETWEEN :start AND :end
        GROUP BY s.content
        ORDER BY COUNT(s.content) DESC""")
    List<Content> findMostFrequentContent(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    Long countByEventTypeAndOccurredAtBetween(EventType eventType, LocalDateTime start, LocalDateTime end);

    Long countUsageEventByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
}

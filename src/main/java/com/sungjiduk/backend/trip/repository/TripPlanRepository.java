package com.sungjiduk.backend.trip.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.trip.entity.TripPlan;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
    /** 내 일정 목록 — 최신 생성 순 */
    List<TripPlan> findByUserIdOrderByIdDesc(Long userId);

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

    long countTripPlanByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

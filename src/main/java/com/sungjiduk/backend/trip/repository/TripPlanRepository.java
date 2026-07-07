package com.sungjiduk.backend.trip.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.sungjiduk.backend.trip.entity.TripPlan;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
    @Query(
        """
        SELECT s.contentId
        FROM TripPlan s
        WHERE s.createdAt BETWEEN :start AND :end
        GROUP BY s.contentId
        ORDER BY COUNT(s.contentId) DESC""")
    List<Long> findMostFrequentContentIdToday(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    long countTripPlanByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

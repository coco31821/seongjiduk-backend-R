package com.sungjiduk.backend.trip.repository;

import java.util.List;

import com.sungjiduk.backend.trip.entity.TripPlan;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
    @Query("""
        SELECT p.contentId\s
        FROM TripPlan p
        GROUP BY p.contentId\s
        ORDER BY COUNT(p.contentId)
        DESC""")
    List<Long> findFrequentContent(Pageable pageable);
}

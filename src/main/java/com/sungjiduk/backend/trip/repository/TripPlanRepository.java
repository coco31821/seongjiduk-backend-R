package com.sungjiduk.backend.trip.repository;

import com.sungjiduk.backend.trip.entity.TripPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
}

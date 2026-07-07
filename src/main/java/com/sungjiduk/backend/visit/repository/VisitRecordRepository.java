package com.sungjiduk.backend.visit.repository;

import com.sungjiduk.backend.visit.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    /** 내 방문 기록 — 최근 방문 순 */
    List<VisitRecord> findByUserIdOrderByVisitedAtDesc(Long userId);
}

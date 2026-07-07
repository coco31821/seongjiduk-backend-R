package com.sungjiduk.backend.ailog.repository;

import com.sungjiduk.backend.ailog.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {

    /** 관리자 통계(C파트)용 — 기간 내 AI 호출 수 */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

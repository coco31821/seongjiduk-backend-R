package com.sungjiduk.backend.event.repository;

import com.sungjiduk.backend.event.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
}

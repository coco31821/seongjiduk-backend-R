package com.sungjiduk.backend.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sungjiduk.backend.content.entity.Content;

public interface ContentRepository extends JpaRepository<Content, Long> {
}

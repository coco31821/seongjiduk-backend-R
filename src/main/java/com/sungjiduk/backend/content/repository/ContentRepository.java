package com.sungjiduk.backend.content.repository;

import com.sungjiduk.backend.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content,Long> {

    @Query("""
        select c from Content c
        where (:category is null or c.category = :category)
            and (:country is null or c.country = :country)
        order by c.id asc
    """)
    List<Content> findContents(
        @Param("category") String category,
        @Param("country") String country
    );

}

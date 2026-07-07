package com.sungjiduk.backend.content.repository;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {

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

    default Content findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
    }
}

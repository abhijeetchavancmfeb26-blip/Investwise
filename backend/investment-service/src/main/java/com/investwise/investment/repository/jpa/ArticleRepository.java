package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Article;
import com.investwise.investment.model.Enums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    @Query("""
           SELECT a FROM Article a
           WHERE a.published = true
             AND (:category IS NULL OR a.category = :category)
             AND (:keyword IS NULL OR :keyword = ''
                  OR LOWER(a.title)   LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))
           """)
    Page<Article> search(@Param("category") Enums.ArticleCategory category,
                         @Param("keyword") String keyword, Pageable pageable);

    List<Article> findTop5ByPublishedTrueOrderByViewCountDesc();

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViews(@Param("id") Long id);
}

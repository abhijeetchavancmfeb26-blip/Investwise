package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    @Query("""
           SELECT r FROM Recommendation r JOIN FETCH r.product
           WHERE r.userId = :userId AND (:goalId IS NULL OR r.goal.id = :goalId)
           ORDER BY r.matchScore DESC
           """)
    List<Recommendation> findLatest(@Param("userId") Long userId, @Param("goalId") Long goalId);

    /** Superseded advice is cleared before a new run, so one live set exists per goal. */
    @Modifying
    @Query("DELETE FROM Recommendation r WHERE r.userId = :userId AND (:goalId IS NULL OR r.goal.id = :goalId)")
    void deleteForUser(@Param("userId") Long userId, @Param("goalId") Long goalId);

    @Query("SELECT COUNT(r) FROM Recommendation r WHERE r.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT r.product.name, COUNT(r) FROM Recommendation r GROUP BY r.product.name ORDER BY COUNT(r) DESC")
    List<Object[]> mostRecommended();
}

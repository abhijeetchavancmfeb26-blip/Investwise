package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Goal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserId(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    @Query("""
           SELECT g FROM Goal g
           WHERE g.userId = :userId
             AND (:status IS NULL OR g.status = :status)
             AND (:type   IS NULL OR g.goalType = :type)
           """)
    Page<Goal> findFiltered(@Param("userId") Long userId,
                            @Param("status") Enums.GoalStatus status,
                            @Param("type") Enums.GoalType type,
                            Pageable pageable);

    long countByUserId(Long userId);

    long countByStatus(Enums.GoalStatus status);

    @Query("SELECT g FROM Goal g WHERE g.status <> 'CANCELLED' AND g.status <> 'ACHIEVED'")
    List<Goal> findTrackable();

    @Query("SELECT g.goalType, COUNT(g) FROM Goal g GROUP BY g.goalType ORDER BY COUNT(g) DESC")
    List<Object[]> countByType();
}

package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("""
           SELECT s FROM Subscription s JOIN FETCH s.plan
           WHERE s.userId = :userId AND s.status = 'ACTIVE'
             AND s.startDate <= CURRENT_DATE AND s.endDate >= CURRENT_DATE
           ORDER BY s.endDate DESC
           """)
    List<Subscription> findActive(@Param("userId") Long userId);

    default Optional<Subscription> findCurrent(Long userId) {
        return findActive(userId).stream().findFirst();
    }

    Page<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Subscription> findByStatus(Enums.SubscriptionStatus status, Pageable pageable);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    long countByStatus(Enums.SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan WHERE s.status = 'ACTIVE' AND s.endDate < :today")
    List<Subscription> findLapsed(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(s.plan.price), 0) FROM Subscription s WHERE s.status = 'ACTIVE'")
    BigDecimal recurringRevenue();

    @Query("SELECT s.plan.name, COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' GROUP BY s.plan.name")
    List<Object[]> countActiveByPlan();
}

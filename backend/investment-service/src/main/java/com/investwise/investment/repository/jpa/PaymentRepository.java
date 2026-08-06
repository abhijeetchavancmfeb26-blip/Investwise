package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByIdAndUserId(Long id, Long userId);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
           SELECT p FROM Payment p
           WHERE (:status IS NULL OR p.status = :status)
             AND (:userId IS NULL OR p.userId = :userId)
           ORDER BY p.createdAt DESC
           """)
    Page<Payment> findFiltered(@Param("status") Enums.PaymentStatus status,
                               @Param("userId") Long userId,
                               Pageable pageable);

    long countByStatus(Enums.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal totalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.createdAt >= :since")
    BigDecimal revenueSince(@Param("since") LocalDateTime since);

    @Query("""
           SELECT FUNCTION('DATE_FORMAT', p.createdAt, '%Y-%m'), COALESCE(SUM(p.amount), 0)
           FROM Payment p WHERE p.status = 'SUCCESS' AND p.createdAt >= :since
           GROUP BY FUNCTION('DATE_FORMAT', p.createdAt, '%Y-%m') ORDER BY 1
           """)
    List<Object[]> revenueByMonth(@Param("since") LocalDateTime since);

    List<Payment> findTop10ByStatusOrderByCreatedAtDesc(Enums.PaymentStatus status);
}

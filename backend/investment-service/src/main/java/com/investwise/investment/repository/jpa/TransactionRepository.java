package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
           SELECT t FROM Transaction t
           WHERE t.userId = :userId
             AND (:type IS NULL OR t.type = :type)
             AND (:from IS NULL OR t.createdAt >= :from)
             AND (:to   IS NULL OR t.createdAt <= :to)
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findFiltered(@Param("userId") Long userId,
                                   @Param("type") Enums.TransactionType type,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}

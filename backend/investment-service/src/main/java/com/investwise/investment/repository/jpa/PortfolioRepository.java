package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    /** Fetch join avoids the N+1 that rendering holdings would otherwise cause. */
    @Query("""
           SELECT DISTINCT p FROM Portfolio p
           LEFT JOIN FETCH p.holdings h
           LEFT JOIN FETCH h.product
           WHERE p.userId = :userId
           """)
    Optional<Portfolio> findByUserIdWithHoldings(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.totalInvested), 0) FROM Portfolio p")
    BigDecimal sumInvested();

    @Query("SELECT COALESCE(SUM(p.currentValue), 0) FROM Portfolio p")
    BigDecimal sumCurrentValue();
}

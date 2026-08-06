package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByIdAndPortfolioUserId(Long id, Long userId);

    @Query("SELECT h FROM Holding h JOIN FETCH h.product WHERE h.units > 0")
    List<Holding> findAllOpen();
}

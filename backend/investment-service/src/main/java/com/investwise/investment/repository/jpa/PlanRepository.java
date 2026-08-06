package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Plan> findByActiveTrueOrderByPriceAsc();
}

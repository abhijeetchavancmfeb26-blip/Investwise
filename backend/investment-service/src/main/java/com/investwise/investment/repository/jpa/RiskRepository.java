package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskRepository extends JpaRepository<RiskAssessment, Long> {

    /** The current profile is simply the most recent submission. */
    Optional<RiskAssessment> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Page<RiskAssessment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT r.profile, COUNT(r) FROM RiskAssessment r GROUP BY r.profile")
    List<Object[]> countByProfile();
}

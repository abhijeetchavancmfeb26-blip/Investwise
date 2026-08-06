package com.investwise.investment.repository.jpa;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Product> findByCategoryAndActiveTrue(Enums.Category category);

    List<Product> findTop6ByActiveTrueOrderByRatingDescExpectedReturnDesc();

    /** One query, every filter optional. Beats a combinatorial explosion of method names. */
    @Query("""
           SELECT p FROM Product p
           WHERE (:activeOnly = false OR p.active = true)
             AND (:keyword IS NULL OR :keyword = ''
                  OR LOWER(p.name)        LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.fundHouse)   LIKE LOWER(CONCAT('%', :keyword, '%')))
             AND (:category  IS NULL OR p.category = :category)
             AND (:riskLevel IS NULL OR p.riskLevel = :riskLevel)
             AND (:minReturn IS NULL OR p.expectedReturn >= :minReturn)
             AND (:maxAmount IS NULL OR p.minInvestment <= :maxAmount)
             AND (:includePremium = true OR p.premiumOnly = false)
           """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("category") Enums.Category category,
                         @Param("riskLevel") Enums.RiskLevel riskLevel,
                         @Param("minReturn") BigDecimal minReturn,
                         @Param("maxAmount") BigDecimal maxAmount,
                         @Param("includePremium") boolean includePremium,
                         @Param("activeOnly") boolean activeOnly,
                         Pageable pageable);

    /** Candidate pool for the recommendation engine. */
    @Query("""
           SELECT p FROM Product p
           WHERE p.active = true
             AND p.minInvestment <= :investable
             AND (:includePremium = true OR p.premiumOnly = false)
           """)
    List<Product> findCandidates(@Param("investable") BigDecimal investable,
                                 @Param("includePremium") boolean includePremium);

    long countByActiveTrue();

    long countByPremiumOnlyTrue();

    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.active = true GROUP BY p.category")
    List<Object[]> countByCategory();

    @Query("SELECT COALESCE(AVG(p.expectedReturn), 0) FROM Product p WHERE p.active = true")
    BigDecimal averageExpectedReturn();
}

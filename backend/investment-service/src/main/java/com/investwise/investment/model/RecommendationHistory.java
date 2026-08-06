package com.investwise.investment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A frozen snapshot of one recommendation run.
 * <p>
 * MongoDB suits this: a nested, variable-length record written once and read
 * whole. Product names are denormalised so the history survives a later
 * catalogue edit — a regulatory expectation as much as a design preference.
 */
@Document(collection = "recommendation_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationHistory {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private Long goalId;
    private String goalTitle;
    private String riskProfile;
    private Integer riskScore;
    private BigDecimal investableAmount;
    private Integer horizonYears;
    private BigDecimal expectedReturn;

    @Builder.Default
    private List<Item> items = List.of();

    @Builder.Default
    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Denormalised line item. */
    public record Item(Long productId, String productCode, String productName, String category,
                       String riskLevel, BigDecimal expectedReturn, BigDecimal allocationPct,
                       BigDecimal amount, BigDecimal matchScore, String rationale) { }
}

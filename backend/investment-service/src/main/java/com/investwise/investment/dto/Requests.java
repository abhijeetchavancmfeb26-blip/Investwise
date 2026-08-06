package com.investwise.investment.dto;

import com.investwise.investment.model.Enums;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Every inbound payload for the Investment Service, as validated records. */
public final class Requests {

    private Requests() { }

    public record Goal(
            @NotBlank @Size(min = 3, max = 120) String title,
            @Size(max = 500) String description,
            @NotNull(message = "Choose a goal type") Enums.GoalType goalType,
            @NotNull @DecimalMin(value = "1000", message = "Target must be at least 1,000")
            @DecimalMax("999999999") BigDecimal targetAmount,
            @DecimalMin(value = "0", message = "Amount saved cannot be negative") BigDecimal currentAmount,
            @DecimalMin(value = "0", message = "Monthly contribution cannot be negative") BigDecimal monthlyContribution,
            @NotNull @Future(message = "The target date must be in the future") LocalDate targetDate,
            Enums.Priority priority) {

        @AssertTrue(message = "Amount saved cannot exceed the target")
        public boolean isWithinTarget() {
            return currentAmount == null || targetAmount == null
                    || currentAmount.compareTo(targetAmount) <= 0;
        }

        @AssertTrue(message = "The target date must be at least one month away")
        public boolean isPlannable() {
            return targetDate == null || !targetDate.isBefore(LocalDate.now().plusMonths(1));
        }
    }

    public record Contribution(
            @NotNull @DecimalMin(value = "1", message = "Contribution must be at least 1") BigDecimal amount,
            @Size(max = 200) String note) { }

    public record RiskQuestionnaire(
            @NotNull @Min(18) @Max(100) Integer age,
            @NotNull @DecimalMin("0") BigDecimal annualIncome,
            @NotNull @DecimalMin("0") BigDecimal monthlySurplus,
            @Min(0) @Max(15) Integer dependents,
            @NotNull @Min(1) @Max(40) Integer horizonYears,
            @NotNull(message = "Select your experience level") Enums.Knowledge knowledgeLevel,
            @NotNull(message = "Tell us how you would react to a fall") Enums.LossTolerance lossTolerance,
            boolean hasEmergencyFund,
            boolean hasHealthInsurance) { }

    public record Recommend(
            Long goalId,
            @NotNull @DecimalMin(value = "500", message = "Investable amount must be at least 500") BigDecimal investableAmount,
            @Min(1) @Max(40) Integer horizonYears) { }

    public record Product(
            @NotBlank @Pattern(regexp = "^[A-Z]{2}-[A-Z]{2}-\\d{3}$", message = "Code must look like IW-EQ-001") String code,
            @NotBlank @Size(min = 3, max = 150) String name,
            @Size(max = 4000) String description,
            @NotNull Enums.Category category,
            @NotNull Enums.RiskLevel riskLevel,
            @NotNull @DecimalMin("0") @DecimalMax(value = "60", message = "A return above 60% is not credible") BigDecimal expectedReturn,
            @NotNull @DecimalMin(value = "100", message = "Minimum investment must be at least 100") BigDecimal minInvestment,
            @Min(0) @Max(360) Integer lockInMonths,
            @Size(max = 120) String fundHouse,
            @DecimalMin("0") @DecimalMax(value = "5", message = "Expense ratio above 5% is not permitted") BigDecimal expenseRatio,
            @NotNull @Min(1) @Max(5) Integer rating,
            boolean premiumOnly,
            boolean active) { }

    public record Holding(
            @NotNull(message = "Choose a product") Long productId,
            Long goalId,
            @NotNull @DecimalMin(value = "100", message = "Investment must be at least 100") BigDecimal amount,
            @NotNull @DecimalMin(value = "0.0001", message = "Price must be greater than zero") BigDecimal buyPrice,
            @NotNull @PastOrPresent(message = "The purchase date cannot be in the future") LocalDate purchaseDate) { }

    public record Redeem(
            @NotNull @DecimalMin(value = "0.0001", message = "Units must be greater than zero") BigDecimal units) { }

    public record Subscribe(
            @NotBlank(message = "Plan code is required") String planCode) { }

    public record VerifyPayment(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature) { }

    public record Article(
            @NotBlank @Size(min = 5, max = 200) String title,
            @Size(max = 500) String summary,
            @NotBlank @Size(min = 50, message = "Content must be at least 50 characters") String content,
            @NotNull Enums.ArticleCategory category,
            @Size(max = 120) String author,
            @Min(1) @Max(120) Integer readMinutes,
            boolean premiumOnly,
            boolean published) { }
}

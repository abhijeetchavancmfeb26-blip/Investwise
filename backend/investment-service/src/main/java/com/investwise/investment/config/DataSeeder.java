package com.investwise.investment.config;

import com.investwise.investment.model.Article;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Plan;
import com.investwise.investment.model.Product;
import com.investwise.investment.repository.jpa.ArticleRepository;
import com.investwise.investment.repository.jpa.PlanRepository;
import com.investwise.investment.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the catalogue, plans and library on first start.
 * Idempotent by code and slug, so it is safe against a database already loaded
 * by seed.sql.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository products;
    private final PlanRepository plans;
    private final ArticleRepository articles;

    private record P(String code, String name, String description, Enums.Category category,
                     Enums.RiskLevel risk, String expectedReturn, String minInvestment,
                     int lockIn, String fundHouse, String expenseRatio, int rating, boolean premium) { }

    private record L(String code, String name, String description, Enums.Tier tier,
                     String price, int months, String features, int maxGoals) { }

    private record A(String title, String summary, String content,
                     Enums.ArticleCategory category, int readMinutes, boolean premium) { }

    @Override
    @Transactional
    public void run(String... args) {
        seedPlans();
        seedProducts();
    }

    private void seedPlans() {
        List.of(
            new L("FREE", "Starter", "Everything you need to begin investing with intent.",
                  Enums.Tier.FREE, "0", 12,
                  "Up to 3 financial goals|Risk assessment|Product catalogue|Portfolio tracking|Educational articles", 3),
            new L("PREMIUM_M", "Premium Monthly", "Advanced analytics and unlimited planning, billed monthly.",
                  Enums.Tier.PREMIUM, "499", 1,
                  "Unlimited goals|Personalised recommendations|Premium-only products|Advanced analytics|PDF reports|Priority support", 999),
            new L("PREMIUM_Y", "Premium Annual", "The full InvestWise experience at two months free.",
                  Enums.Tier.PREMIUM, "4999", 12,
                  "Everything in Premium Monthly|Portfolio health review|Tax insights|Rebalancing plan|Priority support", 999),
            new L("ELITE_Y", "Elite Annual", "White-glove planning with a dedicated advisory desk.",
                  Enums.Tier.ELITE, "14999", 12,
                  "Everything in Premium Annual|Dedicated advisor|Quarterly rebalancing calls|Concierge onboarding", 999)
        ).stream()
         .filter(plan -> !plans.existsByCodeIgnoreCase(plan.code()))
         .forEach(plan -> {
             plans.save(Plan.builder().code(plan.code()).name(plan.name())
                     .description(plan.description()).tier(plan.tier())
                     .price(new BigDecimal(plan.price())).durationMonths(plan.months())
                     .features(plan.features()).maxGoals(plan.maxGoals()).active(true).build());
             log.info("Seeded plan {}", plan.code());
         });
    }

    private void seedProducts() {
        List.of(
            new P("IW-EQ-001", "Bluechip Large Cap Fund", "Diversified large-cap equity fund tracking India's top 100 companies. Suited for long-horizon wealth creation.", Enums.Category.EQUITY, Enums.RiskLevel.MODERATE, "13.50", "5000", 0, "Axis AMC", "1.05", 4, false),
            new P("IW-EQ-002", "Emerging Mid Cap Fund", "Concentrated mid-cap portfolio targeting scalable businesses. Higher volatility, higher upside.", Enums.Category.EQUITY, Enums.RiskLevel.HIGH, "16.80", "5000", 0, "HDFC AMC", "1.42", 4, false),
            new P("IW-EQ-003", "Small Cap Alpha Fund", "Aggressive small-cap fund for a 7+ year horizon and high loss tolerance.", Enums.Category.EQUITY, Enums.RiskLevel.VERY_HIGH, "19.20", "5000", 12, "Nippon India MF", "1.68", 3, true),
            new P("IW-EQ-004", "Nifty 50 Index Fund", "Passively managed fund replicating the Nifty 50 with minimal tracking error and low cost.", Enums.Category.EQUITY, Enums.RiskLevel.MODERATE, "12.40", "1000", 0, "UTI MF", "0.20", 5, false),
            new P("IW-EQ-005", "ELSS Tax Saver Fund", "Equity linked savings scheme with the shortest lock-in among 80C instruments.", Enums.Category.ELSS, Enums.RiskLevel.HIGH, "14.70", "500", 36, "Quant MF", "1.75", 4, false),
            new P("IW-DB-001", "Corporate Bond Fund", "AA+ and above rated corporate debt. Stable accrual income with low duration risk.", Enums.Category.DEBT, Enums.RiskLevel.LOW, "7.60", "5000", 0, "ICICI Prudential AMC", "0.58", 4, false),
            new P("IW-DB-002", "Liquid Overnight Fund", "Parking vehicle for an emergency corpus with next-day redemption.", Enums.Category.DEBT, Enums.RiskLevel.VERY_LOW, "6.40", "1000", 0, "SBI MF", "0.16", 5, false),
            new P("IW-DB-003", "Partner Fixed Deposit", "Corporate fixed deposit, AAA rated. Guaranteed returns, no market linkage.", Enums.Category.DEBT, Enums.RiskLevel.VERY_LOW, "7.25", "10000", 12, "Bajaj Finance", null, 5, false),
            new P("IW-DB-004", "Public Provident Fund", "Government-backed 15 year scheme with EEE tax treatment.", Enums.Category.DEBT, Enums.RiskLevel.VERY_LOW, "7.10", "500", 180, "Government of India", null, 5, false),
            new P("IW-DB-005", "National Pension System", "Retirement scheme with equity/debt auto-choice and an extra 50,000 deduction.", Enums.Category.DEBT, Enums.RiskLevel.MODERATE, "10.20", "1000", 180, "PFRDA", "0.09", 4, false),
            new P("IW-GD-001", "Sovereign Gold Bond", "RBI issued bond paying 2.5% annual interest on top of gold price appreciation.", Enums.Category.GOLD, Enums.RiskLevel.LOW, "9.80", "5000", 60, "Reserve Bank of India", null, 4, false),
            new P("IW-GD-002", "Gold ETF", "Exchange traded fund tracking domestic gold prices. Liquid inflation hedge.", Enums.Category.GOLD, Enums.RiskLevel.LOW, "9.10", "1000", 0, "Nippon India MF", "0.32", 4, false),
            new P("IW-HY-001", "Balanced Advantage Fund", "Shifts between equity and debt based on valuations. Smoother ride across cycles.", Enums.Category.HYBRID, Enums.RiskLevel.MODERATE, "11.60", "5000", 0, "Kotak AMC", "1.12", 4, false),
            new P("IW-HY-002", "Aggressive Hybrid Fund", "65-80% equity with a debt cushion. A first step up from fixed deposits.", Enums.Category.HYBRID, Enums.RiskLevel.MODERATE, "12.90", "5000", 0, "Mirae Asset", "1.24", 4, false),
            new P("IW-RE-001", "REIT Income Portfolio", "Commercial real estate trust generating rental yield plus capital appreciation.", Enums.Category.REAL_ESTATE, Enums.RiskLevel.HIGH, "11.20", "15000", 0, "Embassy REIT", null, 3, true)
        ).stream()
         .filter(p -> !products.existsByCodeIgnoreCase(p.code()))
         .forEach(p -> {
             products.save(Product.builder().code(p.code()).name(p.name()).description(p.description())
                     .category(p.category()).riskLevel(p.risk())
                     .expectedReturn(new BigDecimal(p.expectedReturn()))
                     .minInvestment(new BigDecimal(p.minInvestment()))
                     .lockInMonths(p.lockIn()).fundHouse(p.fundHouse())
                     .expenseRatio(p.expenseRatio() == null ? null : new BigDecimal(p.expenseRatio()))
                     .rating(p.rating()).premiumOnly(p.premium()).active(true).build());
             log.info("Seeded product {}", p.code());
         });
    }


}

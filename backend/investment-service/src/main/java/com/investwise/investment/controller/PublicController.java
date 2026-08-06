package com.investwise.investment.controller;

import com.investwise.investment.common.ApiResponse;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.security.AuthUser;
import com.investwise.investment.service.EducationService;
import com.investwise.investment.service.ProductService;
import com.investwise.investment.service.StatsService;
import com.investwise.investment.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Everything the marketing site renders without an account.
 * <p>
 * Products, plans, articles and calculators were four controllers in the original.
 * They share one characteristic — anonymous read access — so they share one class.
 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "1. Public")
public class PublicController {

    private final ProductService productService;
    private final SubscriptionService subscriptionService;
    private final EducationService educationService;
    private final StatsService statsService;

    // ---------------- products ----------------

    @GetMapping("/products")
    @Operation(summary = "Search the catalogue",
            description = "Premium-only products appear only for callers on a paid plan.")
    public ResponseEntity<ApiResponse<PageResponse<Responses.ProductView>>> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Enums.Category category,
            @RequestParam(required = false) Enums.RiskLevel riskLevel,
            @RequestParam(required = false) BigDecimal minReturn,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "expectedReturn") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal AuthUser user) {

        boolean includePremium = user != null && user.isPremium();
        return ResponseEntity.ok(ApiResponse.ok(productService.search(keyword, category, riskLevel,
                minReturn, maxAmount, includePremium, true, page, size, sortBy, sortDir)));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Fetch one product")
    public ResponseEntity<ApiResponse<Responses.ProductView>> product(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.get(id)));
    }

    @GetMapping("/products/code/{code}")
    @Operation(summary = "Fetch one product by catalogue code")
    public ResponseEntity<ApiResponse<Responses.ProductView>> productByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getByCode(code)));
    }

    @GetMapping("/products/featured")
    @Operation(summary = "Top rated products for the home page")
    public ResponseEntity<ApiResponse<List<Responses.ProductView>>> featured() {
        return ResponseEntity.ok(ApiResponse.ok(productService.featured()));
    }

    @GetMapping("/products/category/{category}")
    @Operation(summary = "Every active product in one category")
    public ResponseEntity<ApiResponse<List<Responses.ProductView>>> byCategory(
            @PathVariable Enums.Category category) {
        return ResponseEntity.ok(ApiResponse.ok(productService.byCategory(category)));
    }

    @GetMapping("/metadata")
    @Operation(summary = "Enum values that populate every filter and dropdown in the UI")
    public ResponseEntity<ApiResponse<Map<String, Object>>> metadata() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "categories", Arrays.stream(Enums.Category.values())
                        .map(c -> Map.of("value", c.name(), "label", c.label(),
                                         "assetClass", c.assetClass().name())).toList(),
                "riskLevels", Arrays.stream(Enums.RiskLevel.values())
                        .map(r -> Map.of("value", r.name(), "score", r.score())).toList(),
                "goalTypes", Arrays.stream(Enums.GoalType.values())
                        .map(g -> Map.of("value", g.name(), "label", g.label(),
                                         "horizonYears", g.horizonYears())).toList(),
                "priorities", Arrays.stream(Enums.Priority.values()).map(Enum::name).toList(),
                "knowledgeLevels", Arrays.stream(Enums.Knowledge.values()).map(Enum::name).toList(),
                "lossTolerances", List.of(
                        Map.of("value", "SELL_EVERYTHING", "label", "Sell everything to stop further losses"),
                        Map.of("value", "SELL_SOME", "label", "Sell some holdings to reduce exposure"),
                        Map.of("value", "HOLD", "label", "Hold and wait for a recovery"),
                        Map.of("value", "BUY_MORE", "label", "Buy more while prices are lower")),
                "articleCategories", Arrays.stream(Enums.ArticleCategory.values()).map(Enum::name).toList(),
                "reportTypes", Arrays.stream(Enums.ReportType.values())
                        .map(r -> Map.of("value", r.name(), "label", r.label(),
                                         "premiumOnly", r.premiumOnly())).toList())));
    }

    // ---------------- plans ----------------

    @GetMapping("/plans")
    @Operation(summary = "Public pricing table")
    public ResponseEntity<ApiResponse<List<Responses.PlanView>>> plans() {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.activePlans()));
    }

    @GetMapping("/plans/{code}")
    @Operation(summary = "Fetch one plan by code")
    public ResponseEntity<ApiResponse<Responses.PlanView>> plan(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.plan(code)));
    }

    // ---------------- education ----------------

    @GetMapping("/education")
    @Operation(summary = "Browse the library")
    public ResponseEntity<ApiResponse<PageResponse<Responses.ArticleView>>> articles(
            @RequestParam(required = false) Enums.ArticleCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return ResponseEntity.ok(ApiResponse.ok(educationService.list(category, keyword, page, size)));
    }

    @GetMapping("/education/popular")
    @Operation(summary = "Most read articles")
    public ResponseEntity<ApiResponse<List<Responses.ArticleView>>> popularArticles() {
        return ResponseEntity.ok(ApiResponse.ok(educationService.popular()));
    }

    @GetMapping("/education/{slug}")
    @Operation(summary = "Read one article",
            description = "Premium-only articles require an active paid plan.")
    public ResponseEntity<ApiResponse<Responses.ArticleView>> article(
            @PathVariable String slug, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(
                educationService.read(slug, user != null && user.isPremium())));
    }

    // ---------------- calculators ----------------

    @GetMapping("/calculators/sip")
    @Operation(summary = "Project a monthly SIP forward")
    public ResponseEntity<ApiResponse<Responses.CalculatorView>> sip(
            @RequestParam @DecimalMin("100") BigDecimal monthlyAmount,
            @RequestParam(defaultValue = "12") @DecimalMin("1") BigDecimal annualRate,
            @RequestParam(defaultValue = "10") @Min(1) @Max(40) int years) {
        return ResponseEntity.ok(ApiResponse.ok(statsService.sip(monthlyAmount, annualRate, years)));
    }

    @GetMapping("/calculators/lumpsum")
    @Operation(summary = "Project a one-time investment forward")
    public ResponseEntity<ApiResponse<Responses.CalculatorView>> lumpsum(
            @RequestParam @DecimalMin("1000") BigDecimal principal,
            @RequestParam(defaultValue = "12") @DecimalMin("1") BigDecimal annualRate,
            @RequestParam(defaultValue = "10") @Min(1) @Max(40) int years) {
        return ResponseEntity.ok(ApiResponse.ok(statsService.lumpsum(principal, annualRate, years)));
    }

    @GetMapping("/calculators/goal")
    @Operation(summary = "Work out the monthly investment a goal requires",
            description = "Inflates the target first, so the answer funds it in future rupees.")
    public ResponseEntity<ApiResponse<Responses.CalculatorView>> goal(
            @RequestParam @DecimalMin("10000") BigDecimal targetAmount,
            @RequestParam(defaultValue = "12") @DecimalMin("1") BigDecimal annualRate,
            @RequestParam(defaultValue = "10") @Min(1) @Max(40) int years,
            @RequestParam(defaultValue = "6.0") double inflationPct) {
        return ResponseEntity.ok(ApiResponse.ok(
                statsService.goal(targetAmount, annualRate, years, inflationPct)));
    }
}

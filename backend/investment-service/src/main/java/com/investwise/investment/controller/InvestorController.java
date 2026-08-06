package com.investwise.investment.controller;

import com.investwise.investment.common.ApiResponse;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.RecommendationHistory;
import com.investwise.investment.security.AuthUser;
import com.investwise.investment.service.GoalService;
import com.investwise.investment.service.PortfolioService;
import com.investwise.investment.service.RecommendationService;
import com.investwise.investment.service.ReportService;
import com.investwise.investment.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the signed-in investor does: goals, risk, recommendations, portfolio
 * and reports. Five controllers in the original, one here — they all operate on
 * the same principal and share the same authorisation rule.
 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "2. Investor")
public class InvestorController {

    private final GoalService goalService;
    private final RiskService riskService;
    private final RecommendationService recommendationService;
    private final PortfolioService portfolioService;
    private final ReportService reportService;

    // ---------------- goals ----------------

    @PostMapping("/goals")
    @Operation(summary = "Create a financial goal",
            description = "Free plans are limited to three goals; premium plans are unlimited.")
    public ResponseEntity<ApiResponse<Responses.GoalView>> createGoal(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody Requests.Goal request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(goalService.create(user, request), "Goal created"));
    }

    @GetMapping("/goals")
    @Operation(summary = "List goals with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<Responses.GoalView>>> goals(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Enums.GoalStatus status,
            @RequestParam(required = false) Enums.GoalType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(goalService.list(user.id(), status, type, page, size)));
    }

    @GetMapping("/goals/all")
    @Operation(summary = "Every goal, ordered by priority then urgency")
    public ResponseEntity<ApiResponse<List<Responses.GoalView>>> allGoals(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(goalService.listAll(user.id())));
    }

    @GetMapping("/goals/{id}")
    @Operation(summary = "Fetch one goal")
    public ResponseEntity<ApiResponse<Responses.GoalView>> goal(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(goalService.get(id, user.id())));
    }

    @PutMapping("/goals/{id}")
    @Operation(summary = "Update a goal")
    public ResponseEntity<ApiResponse<Responses.GoalView>> updateGoal(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody Requests.Goal request) {
        return ResponseEntity.ok(ApiResponse.ok(goalService.update(id, user, request), "Goal updated"));
    }

    @PostMapping("/goals/{id}/contribute")
    @Operation(summary = "Record a contribution towards a goal")
    public ResponseEntity<ApiResponse<Responses.GoalView>> contribute(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody Requests.Contribution request) {
        Responses.GoalView goal = goalService.contribute(id, user, request);
        return ResponseEntity.ok(ApiResponse.ok(goal,
                goal.status() == Enums.GoalStatus.ACHIEVED
                        ? "Contribution recorded. You have reached this goal."
                        : "Contribution recorded"));
    }

    @DeleteMapping("/goals/{id}")
    @Operation(summary = "Delete a goal")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        goalService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.message("Goal deleted"));
    }

    // ---------------- risk ----------------

    @PostMapping("/risk/assess")
    @Operation(summary = "Submit the risk questionnaire",
            description = "Returns the profile, the strategic allocation and a per-factor breakdown.")
    public ResponseEntity<ApiResponse<Responses.RiskView>> assess(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody Requests.RiskQuestionnaire request) {
        Responses.RiskView view = riskService.assess(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(view,
                "You are a %s investor".formatted(view.profile().name().toLowerCase())));
    }

    @GetMapping("/risk/me")
    @Operation(summary = "Fetch the current risk profile")
    public ResponseEntity<ApiResponse<Responses.RiskView>> currentRisk(
            @AuthenticationPrincipal AuthUser user) {
        return riskService.current(user.id())
                .map(view -> ResponseEntity.ok(ApiResponse.ok(view)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null,
                        "No assessment on file yet. Complete the questionnaire to get started.")));
    }

    @GetMapping("/risk/history")
    @Operation(summary = "Every past assessment, newest first")
    public ResponseEntity<ApiResponse<PageResponse<Responses.RiskView>>> riskHistory(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(riskService.history(user.id(), page, size)));
    }

    // ---------------- recommendations ----------------

    @PostMapping("/recommendations/generate")
    @Operation(summary = "Generate a personalised basket",
            description = "Requires a completed risk assessment. Scores the catalogue, then allocates "
                    + "the investable amount across asset classes.")
    public ResponseEntity<ApiResponse<Responses.RecommendationView>> generate(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody Requests.Recommend request) {
        Responses.RecommendationView view = recommendationService.generate(user, request);
        return ResponseEntity.ok(ApiResponse.ok(view,
                "Generated %d recommendations".formatted(view.items().size())));
    }

    @GetMapping("/recommendations/latest")
    @Operation(summary = "Fetch the most recent basket")
    public ResponseEntity<ApiResponse<Responses.RecommendationView>> latest(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long goalId) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.latest(user.id(), goalId)));
    }

    @GetMapping("/recommendations/history")
    @Operation(summary = "Immutable history of every recommendation run")
    public ResponseEntity<ApiResponse<PageResponse<RecommendationHistory>>> recommendationHistory(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.history(user.id(), page, size)));
    }

    // ---------------- portfolio ----------------

    @GetMapping("/portfolio")
    @Operation(summary = "Holdings with allocation breakdown")
    public ResponseEntity<ApiResponse<Responses.PortfolioView>> portfolio(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.get(user.id())));
    }

    @GetMapping("/portfolio/dashboard")
    @Operation(summary = "Everything the investor dashboard renders, in one call")
    public ResponseEntity<ApiResponse<Responses.DashboardView>> dashboard(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.dashboard(user)));
    }

    @GetMapping("/portfolio/analytics")
    @Operation(summary = "Premium analytics, including the rebalancing plan",
            description = "Requires an active Premium or Elite subscription.")
    public ResponseEntity<ApiResponse<Responses.AnalyticsView>> analytics(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.analytics(user)));
    }

    @PostMapping("/portfolio/holdings")
    @Operation(summary = "Record a purchase into the portfolio")
    public ResponseEntity<ApiResponse<Responses.HoldingView>> addHolding(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody Requests.Holding request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(portfolioService.addHolding(user, request), "Holding added"));
    }

    @PostMapping("/portfolio/holdings/{id}/redeem")
    @Operation(summary = "Redeem units from a holding")
    public ResponseEntity<ApiResponse<Responses.HoldingView>> redeem(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody Requests.Redeem request) {
        return ResponseEntity.ok(ApiResponse.ok(
                portfolioService.redeem(id, user, request), "Redemption recorded"));
    }

    @DeleteMapping("/portfolio/holdings/{id}")
    @Operation(summary = "Remove a holding entirely")
    public ResponseEntity<ApiResponse<Void>> removeHolding(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        portfolioService.removeHolding(id, user.id());
        return ResponseEntity.ok(ApiResponse.message("Holding removed"));
    }

    @GetMapping("/portfolio/transactions")
    @Operation(summary = "Transaction ledger with type and date filters")
    public ResponseEntity<ApiResponse<PageResponse<Responses.TransactionView>>> transactions(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Enums.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                portfolioService.transactions(user.id(), type, from, to, page, size)));
    }

    // ---------------- reports ----------------

    @GetMapping("/reports/{type}/{format}")
    @Operation(summary = "Download a report as PDF or CSV")
    public ResponseEntity<Resource> report(@AuthenticationPrincipal AuthUser user,
                                           @PathVariable Enums.ReportType type,
                                           @PathVariable String format) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? reportService.pdf(user, type) : reportService.csv(user, type);
        String filename = reportService.filename(type, pdf ? "pdf" : "csv");

        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}

package com.investwise.investment.controller;

import com.investwise.investment.common.ApiResponse;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.service.EducationService;
import com.investwise.investment.service.GoalService;
import com.investwise.investment.service.PortfolioService;
import com.investwise.investment.service.ProductService;
import com.investwise.investment.service.StatsService;
import com.investwise.investment.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Investment-side administration.
 * <p>
 * The gateway routes {@code /admin/users}, {@code /admin/contact-messages} and
 * {@code /admin/user-stats} to the User Service; everything else under
 * {@code /api/v1/admin} arrives here.
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "4. Administration")
public class AdminController {

    private final ProductService products;
    private final EducationService education;
    private final SubscriptionService billing;
    private final StatsService stats;
    private final PortfolioService portfolios;
    private final GoalService goals;

    // ---------------- dashboard ----------------

    @GetMapping("/stats")
    @Operation(summary = "Platform-wide investment statistics")
    public ResponseEntity<ApiResponse<Responses.AdminStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(stats.adminStats()));
    }

    // ---------------- products ----------------

    @GetMapping("/products")
    @Operation(summary = "Search every product, including inactive and premium-only ones")
    public ResponseEntity<ApiResponse<PageResponse<Responses.ProductView>>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Enums.Category category,
            @RequestParam(required = false) Enums.RiskLevel riskLevel,
            @RequestParam(required = false) BigDecimal minReturn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(products.search(keyword, category, riskLevel,
                minReturn, null, true, false, page, size, "createdAt", "desc")));
    }

    @PostMapping("/products")
    @Operation(summary = "Add a product to the catalogue")
    public ResponseEntity<ApiResponse<Responses.ProductView>> createProduct(
            @Valid @RequestBody Requests.Product request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(products.create(request), "Product created"));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ApiResponse<Responses.ProductView>> updateProduct(
            @PathVariable Long id, @Valid @RequestBody Requests.Product request) {
        return ResponseEntity.ok(ApiResponse.ok(products.update(id, request), "Product updated"));
    }

    @PatchMapping("/products/{id}/toggle")
    @Operation(summary = "Activate or deactivate a product")
    public ResponseEntity<ApiResponse<Responses.ProductView>> toggleProduct(@PathVariable Long id) {
        Responses.ProductView product = products.toggle(id);
        return ResponseEntity.ok(ApiResponse.ok(product,
                product.active() ? "Product is now active" : "Product is now inactive"));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Delete a product",
            description = "Refused while any investor holds it; deactivate instead.")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        products.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Product deleted"));
    }

    // ---------------- content ----------------

    @PostMapping("/education")
    @Operation(summary = "Publish an educational article")
    public ResponseEntity<ApiResponse<Responses.ArticleView>> createArticle(
            @Valid @RequestBody Requests.Article request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(education.create(request), "Article published"));
    }

    @PutMapping("/education/{id}")
    @Operation(summary = "Update an article")
    public ResponseEntity<ApiResponse<Responses.ArticleView>> updateArticle(
            @PathVariable Long id, @Valid @RequestBody Requests.Article request) {
        return ResponseEntity.ok(ApiResponse.ok(education.update(id, request), "Article updated"));
    }

    @DeleteMapping("/education/{id}")
    @Operation(summary = "Delete an article")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long id) {
        education.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Article deleted"));
    }

    // ---------------- money ----------------

    @GetMapping("/subscriptions")
    @Operation(summary = "List subscriptions, optionally filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<Responses.SubscriptionView>>> subscriptions(
            @RequestParam(required = false) Enums.SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(billing.adminList(status, page, size)));
    }

    @GetMapping("/payments")
    @Operation(summary = "List payments with status and user filters")
    public ResponseEntity<ApiResponse<PageResponse<Responses.PaymentView>>> payments(
            @RequestParam(required = false) Enums.PaymentStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(billing.adminPayments(status, userId, page, size)));
    }

    // ---------------- maintenance ----------------

    @PostMapping("/maintenance/run")
    @Operation(summary = "Run the nightly maintenance jobs on demand",
            description = "Marks holdings to market, refreshes goal statuses and expires lapsed subscriptions.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> runMaintenance() {
        Map<String, Integer> result = Map.of(
                "holdingsRevalued", portfolios.refreshMarketValues(),
                "goalsRefreshed", goals.refreshAllStatuses(),
                "subscriptionsExpired", billing.expireLapsed());
        return ResponseEntity.ok(ApiResponse.ok(result, "Maintenance complete"));
    }
}

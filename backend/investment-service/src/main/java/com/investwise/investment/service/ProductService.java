package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Product;
import com.investwise.investment.repository.jpa.HoldingRepository;
import com.investwise.investment.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** The product catalogue: browsing for investors, CRUD for administrators. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;
    private final HoldingRepository holdings;

    @Transactional(readOnly = true)
    public PageResponse<Responses.ProductView> search(String keyword, Enums.Category category,
                                                      Enums.RiskLevel riskLevel, BigDecimal minReturn,
                                                      BigDecimal maxAmount, boolean includePremium,
                                                      boolean activeOnly, int page, int size,
                                                      String sortBy, String sortDir) {
        return PageResponse.of(products.search(
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                category, riskLevel, minReturn, maxAmount, includePremium, activeOnly,
                pageable(page, size, sortBy, sortDir)), Responses.ProductView::from);
    }

    @Transactional(readOnly = true)
    public Responses.ProductView get(Long id) {
        return Responses.ProductView.from(entity(id));
    }

    @Transactional(readOnly = true)
    public Responses.ProductView getByCode(String code) {
        return products.findByCodeIgnoreCase(code)
                .map(Responses.ProductView::from)
                .orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional(readOnly = true)
    public Product entity(Long id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional(readOnly = true)
    public List<Responses.ProductView> featured() {
        return products.findTop6ByActiveTrueOrderByRatingDescExpectedReturnDesc().stream()
                .map(Responses.ProductView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.ProductView> byCategory(Enums.Category category) {
        return products.findByCategoryAndActiveTrue(category).stream()
                .sorted()   // natural ordering: highest expected return first
                .map(Responses.ProductView::from).toList();
    }

    // ---------------- administration ----------------

    @Transactional
    public Responses.ProductView create(Requests.Product request) {
        if (products.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("A product with code " + request.code() + " already exists");
        }
        Product saved = products.save(Product.builder()
                .code(request.code().toUpperCase())
                .name(request.name().trim())
                .description(request.description())
                .category(request.category())
                .riskLevel(request.riskLevel())
                .expectedReturn(request.expectedReturn())
                .minInvestment(request.minInvestment())
                .lockInMonths(request.lockInMonths() == null ? 0 : request.lockInMonths())
                .fundHouse(request.fundHouse())
                .expenseRatio(request.expenseRatio())
                .rating(request.rating())
                .premiumOnly(request.premiumOnly())
                .active(request.active())
                .build());

        log.info("Product {} created", saved.getCode());
        return Responses.ProductView.from(saved);
    }

    @Transactional
    public Responses.ProductView update(Long id, Requests.Product request) {
        Product product = entity(id);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setRiskLevel(request.riskLevel());
        product.setExpectedReturn(request.expectedReturn());
        product.setMinInvestment(request.minInvestment());
        product.setLockInMonths(request.lockInMonths() == null ? 0 : request.lockInMonths());
        product.setFundHouse(request.fundHouse());
        product.setExpenseRatio(request.expenseRatio());
        product.setRating(request.rating());
        product.setPremiumOnly(request.premiumOnly());
        product.setActive(request.active());
        return Responses.ProductView.from(products.save(product));
    }

    @Transactional
    public Responses.ProductView toggle(Long id) {
        Product product = entity(id);
        product.setActive(!product.isActive());
        return Responses.ProductView.from(products.save(product));
    }

    /**
     * Deletion is refused while investors still hold the product: removing it would
     * orphan real positions. Deactivating is the correct action instead.
     */
    @Transactional
    public void delete(Long id) {
        Product product = entity(id);
        boolean held = holdings.findAllOpen().stream()
                .anyMatch(holding -> holding.getProduct().getId().equals(id));

        if (held) {
            throw ApiException.forbidden(
                    "This product is held in one or more portfolios. Deactivate it instead of deleting it.");
        }
        products.delete(product);
        log.warn("Product {} deleted", product.getCode());
    }

    /** Clamps the page size and whitelists nothing beyond what the entity exposes. */
    static Pageable pageable(int page, int size, String sortBy, String sortDir) {
        String property = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(direction, property));
    }

    static Pageable pageable(int page, int size) {
        return pageable(page, size, "createdAt", "desc");
    }
}

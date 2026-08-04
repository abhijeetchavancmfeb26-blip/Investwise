package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Article;
import com.investwise.investment.model.Enums;
import com.investwise.investment.repository.jpa.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** The educational library. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EducationService {

    private final ArticleRepository articles;

    @Transactional(readOnly = true)
    public PageResponse<Responses.ArticleView> list(Enums.ArticleCategory category, String keyword,
                                                    int page, int size) {
        return PageResponse.of(
                articles.search(category, (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                        ProductService.pageable(page, size)),
                // Summaries only; shipping full bodies in a list wastes bandwidth
                article -> Responses.ArticleView.from(article, false));
    }

    @Transactional
    public Responses.ArticleView read(String slug, boolean premiumMember) {
        Article article = articles.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> ApiException.notFound("Article"));

        if (article.isPremiumOnly() && !premiumMember) {
            throw ApiException.forbidden(
                    "\"%s\" is available to Premium members. Upgrade to read the full article."
                            .formatted(article.getTitle()));
        }
        articles.incrementViews(article.getId());
        return Responses.ArticleView.from(article, true);
    }

    @Transactional(readOnly = true)
    public List<Responses.ArticleView> popular() {
        return articles.findTop5ByPublishedTrueOrderByViewCountDesc().stream()
                .map(article -> Responses.ArticleView.from(article, false)).toList();
    }

    @Transactional
    public Responses.ArticleView create(Requests.Article request) {
        Article article = Article.builder()
                .title(request.title().trim())
                .slug(uniqueSlug(Article.slugify(request.title())))
                .summary(request.summary())
                .content(request.content())
                .category(request.category())
                .author(request.author())
                .readMinutes(Optional.ofNullable(request.readMinutes()).orElse(5))
                .premiumOnly(request.premiumOnly())
                .published(request.published())
                .build();

        log.info("Article \"{}\" published", article.getTitle());
        return Responses.ArticleView.from(articles.save(article), true);
    }

    @Transactional
    public Responses.ArticleView update(Long id, Requests.Article request) {
        Article article = articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));

        article.setTitle(request.title().trim());
        article.setSummary(request.summary());
        article.setContent(request.content());
        article.setCategory(request.category());
        article.setAuthor(request.author());
        Optional.ofNullable(request.readMinutes()).ifPresent(article::setReadMinutes);
        article.setPremiumOnly(request.premiumOnly());
        article.setPublished(request.published());

        return Responses.ArticleView.from(articles.save(article), true);
    }

    @Transactional
    public void delete(Long id) {
        if (!articles.existsById(id)) {
            throw ApiException.notFound("Article");
        }
        articles.deleteById(id);
    }

    /** Appends a numeric suffix until the slug is free, so titles may repeat. */
    private String uniqueSlug(String base) {
        String candidate = base;
        int suffix = 2;
        while (articles.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}

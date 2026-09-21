package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.NewsRequest;
import com.example.EV_finder_api.dto.NewsResponse;
import com.example.EV_finder_api.entity.FuelType;
import com.example.EV_finder_api.entity.News;
import com.example.EV_finder_api.entity.NewsCategory;
import com.example.EV_finder_api.entity.Role;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.FuelStationInventoryRepository;
import com.example.EV_finder_api.repository.NewsRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.NewsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Energy & fuel news feed. Same nation-wide list for every user (no location
 * filtering by design). Only published items are visible to non-admin users;
 * admins may additionally list/manage drafts.
 *
 * Article text is demo/local content, but fuel-price articles embed price
 * tokens like {DIESEL} that are rendered from the existing
 * fuel_station_inventories table on every read — when an operator updates a
 * pump price, the news feed and detail screen reflect it automatically.
 */
@Service
@Transactional
public class NewsServiceImpl implements NewsService {

    /** Price placeholders allowed inside demo article text. */
    private static final Map<String, FuelType> PRICE_TOKENS = Map.of(
            "{DIESEL}", FuelType.DIESEL,
            "{PETROL}", FuelType.PETROL,
            "{OCTANE}", FuelType.OCTANE,
            "{LPG}", FuelType.LPG);

    private final NewsRepository newsRepository;
    private final FuelStationInventoryRepository fuelInventoryRepository;
    private final CurrentUserProvider currentUserProvider;

    public NewsServiceImpl(NewsRepository newsRepository,
                           FuelStationInventoryRepository fuelInventoryRepository,
                           CurrentUserProvider currentUserProvider) {
        this.newsRepository = newsRepository;
        this.fuelInventoryRepository = fuelInventoryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsResponse> list(NewsCategory category, boolean includeUnpublished) {
        boolean adminView = includeUnpublished && isAdmin();
        List<News> news;
        if (adminView) {
            news = newsRepository.findAllForAdmin(category);
        } else if (category != null) {
            news = newsRepository.findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(category);
        } else {
            news = newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc();
        }
        Map<FuelType, BigDecimal> prices = hasPriceTokens(news) ? currentFuelPrices() : Map.of();
        return news.stream().map(n -> render(n, prices)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NewsResponse details(String newsId) {
        News news = getNews(newsId);
        // drafts are admin-only: normal users get a 404, never a content leak
        if (!Boolean.TRUE.equals(news.getIsPublished()) && !isAdmin()) {
            throw new ResourceNotFoundException("News not found: " + newsId);
        }
        return render(news, hasPriceTokens(List.of(news)) ? currentFuelPrices() : Map.of());
    }

    @Override
    public NewsResponse create(NewsRequest request) {
        News news = News.builder()
                .title(request.title())
                .shortDescription(request.shortDescription())
                .content(request.content())
                .category(request.category())
                .source(request.source())
                .sourceUrl(request.sourceUrl())
                .imageUrl(request.imageUrl())
                .publishedAt(request.publishedAt())
                .isPublished(request.isPublished())
                .build();
        return render(newsRepository.save(news));
    }

    @Override
    public NewsResponse update(String newsId, NewsRequest request) {
        News news = getNews(newsId);
        news.setTitle(request.title());
        news.setShortDescription(request.shortDescription());
        news.setContent(request.content());
        news.setCategory(request.category());
        news.setSource(request.source());
        news.setSourceUrl(request.sourceUrl());
        news.setImageUrl(request.imageUrl());
        news.setPublishedAt(request.publishedAt());
        news.setIsPublished(request.isPublished());
        return render(newsRepository.save(news));
    }

    @Override
    public NewsResponse setPublished(String newsId, boolean published) {
        News news = getNews(newsId);
        news.setIsPublished(published);
        return render(newsRepository.save(news));
    }

    @Override
    public void delete(String newsId) {
        newsRepository.delete(getNews(newsId));
    }

    // ── live fuel-price rendering ────────────────────────────────────────────

    private NewsResponse render(News news) {
        return render(news, hasPriceTokens(List.of(news)) ? currentFuelPrices() : Map.of());
    }

    private NewsResponse render(News news, Map<FuelType, BigDecimal> prices) {
        NewsResponse base = NewsResponse.from(news);
        return new NewsResponse(base.id(),
                replaceTokens(base.title(), prices),
                replaceTokens(base.shortDescription(), prices),
                replaceTokens(base.content(), prices),
                base.category(), base.source(), base.sourceUrl(), base.imageUrl(),
                base.publishedAt(), base.isPublished(), base.createdAt(), base.updatedAt());
    }

    /** Tokens resolve to the latest BDT/liter price; "N/A" when no station sells that fuel. */
    private String replaceTokens(String text, Map<FuelType, BigDecimal> prices) {
        if (text == null || !text.contains("{")) return text;
        String rendered = text;
        for (Map.Entry<String, FuelType> token : PRICE_TOKENS.entrySet()) {
            if (!rendered.contains(token.getKey())) continue;
            BigDecimal price = prices.get(token.getValue());
            rendered = rendered.replace(token.getKey(),
                    price == null ? "N/A" : price.stripTrailingZeros().toPlainString());
        }
        return rendered;
    }

    /** Current pump price per fuel type: the most recently updated inventory row. */
    private Map<FuelType, BigDecimal> currentFuelPrices() {
        Map<FuelType, BigDecimal> prices = new EnumMap<>(FuelType.class);
        for (FuelType type : FuelType.values()) {
            fuelInventoryRepository.findFirstByFuelTypeOrderByUpdatedAtDesc(type)
                    .ifPresent(inv -> prices.put(type, inv.getPricePerLiter()));
        }
        return prices;
    }

    private boolean hasPriceTokens(List<News> items) {
        for (News n : items) {
            if ((n.getTitle() != null && n.getTitle().contains("{"))
                    || (n.getShortDescription() != null && n.getShortDescription().contains("{"))
                    || (n.getContent() != null && n.getContent().contains("{"))) {
                return true;
            }
        }
        return false;
    }

    private News getNews(String newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News not found: " + newsId));
    }

    private boolean isAdmin() {
        return currentUserProvider.getCurrentUser().getRole() == Role.ADMIN;
    }
}

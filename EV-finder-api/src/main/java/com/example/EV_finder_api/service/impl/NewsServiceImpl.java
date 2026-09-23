package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.NewsResponse;
import com.example.EV_finder_api.entity.NewsArticle;
import com.example.EV_finder_api.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Energy / fuel / EV news for the app.
 *
 * Headlines come from Google News' free RSS search (no API key, no billing) and
 * are cached in the database, so the section renders instantly and keeps working
 * if the feed is briefly unreachable. Nothing is invented: when the feed cannot
 * be reached and the cache is empty, the section is simply empty.
 */
@Service
public class NewsServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    /** Category → search phrase. Order is preserved in the UI. */
    private static final Map<String, String> QUERIES = new LinkedHashMap<>() {{
        put("FUEL", "fuel price Bangladesh petrol diesel octane");
        put("LPG", "LPG gas price Bangladesh cylinder");
        put("EV", "electric vehicle charging Bangladesh EV");
        put("POLICY", "Bangladesh power energy ministry policy fuel");
    }};

    private static final Duration FRESH_FOR = Duration.ofMinutes(30);

    private final NewsArticleRepository newsRepository;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Guards against overlapping refreshes (startup + first request). */
    private final Object refreshLock = new Object();

    public NewsServiceImpl(NewsArticleRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    /** Warm the cache in the background so the first screen isn't empty. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread t = new Thread(this::refresh, "news-warmup");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Latest headlines, optionally filtered by category.
     * Refreshes synchronously only when nothing is cached yet (first run), so
     * subsequent calls stay fast.
     */
    @Transactional
    public List<NewsResponse> latest(String category, int limit) {
        boolean empty = newsRepository.count() == 0;
        if (empty) {
            refresh(); // first ever call — worth the extra latency
        } else if (isStale()) {
            Thread t = new Thread(this::refresh, "news-refresh");
            t.setDaemon(true);
            t.start();
        }

        String cat = (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category))
                ? null : category.toUpperCase();
        List<NewsArticle> articles = cat == null
                ? newsRepository.findTop60ByOrderByPublishedAtDesc()
                : newsRepository.findTop40ByCategoryOrderByPublishedAtDesc(cat);
        return articles.stream().limit(limit).map(NewsResponse::from).toList();
    }

    private boolean isStale() {
        return newsRepository.findFirstByOrderByFetchedAtDesc()
                .map(a -> a.getFetchedAt().isBefore(LocalDateTime.now().minus(FRESH_FOR)))
                .orElse(true);
    }

    /** Pulls every category's feed and stores headlines we don't have yet. */
    public int refresh() {
        synchronized (refreshLock) {
            int added = 0;
            for (Map.Entry<String, String> entry : QUERIES.entrySet()) {
                for (NewsArticle article : fetch(entry.getKey(), entry.getValue())) {
                    try {
                        if (newsRepository.findByLink(article.getLink()).isEmpty()) {
                            newsRepository.save(article);
                            added++;
                        }
                    } catch (Exception e) {
                        log.debug("Skipping duplicate news link {}: {}", article.getLink(), e.getMessage());
                    }
                }
            }
            prune();
            log.info("[News] refresh complete — {} new headline(s)", added);
            return added;
        }
    }

    /** Keeps the cache bounded so the table doesn't grow forever. */
    private void prune() {
        List<NewsArticle> all = newsRepository.findTop60ByOrderByPublishedAtDesc();
        if (newsRepository.count() > 120) {
            List<NewsArticle> keep = new ArrayList<>(all);
            newsRepository.findAll().stream()
                    .filter(a -> !keep.contains(a))
                    .forEach(newsRepository::delete);
        }
    }

    private List<NewsArticle> fetch(String category, String query) {
        List<NewsArticle> out = new ArrayList<>();
        String url = "https://news.google.com/rss/search?q="
                + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&hl=en-BD&gl=BD&ceid=BD:en";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "EV-Finder/1.0 (university course project)")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("[News] {} feed returned HTTP {}", category, response.statusCode());
                return out;
            }
            out.addAll(parse(category, response.body()));
        } catch (Exception e) {
            log.warn("[News] {} feed unavailable: {}", category, e.getMessage());
        }
        return out;
    }

    /** Minimal RSS 2.0 parsing — items with title/link/pubDate/source/description. */
    private List<NewsArticle> parse(String category, byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // harden against XXE; feeds are third-party input
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml));
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        List<NewsArticle> articles = new ArrayList<>();
        for (int i = 0; i < items.getLength() && i < 25; i++) {
            Node node = items.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;

            String title = text(el, "title");
            String link = text(el, "link");
            if (title == null || title.isBlank() || link == null || link.isBlank()) continue;

            articles.add(NewsArticle.builder()
                    .title(trim(title, 380))
                    .link(trim(link, 580))
                    .source(trim(firstNonBlank(text(el, "source"), "Google News"), 140))
                    .category(category)
                    .summary(trim(stripHtml(text(el, "description")), 480))
                    .publishedAt(parseDate(text(el, "pubDate")))
                    .fetchedAt(LocalDateTime.now())
                    .build());
        }
        return articles;
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0) == null) return null;
        return nodes.item(0).getTextContent();
    }

    private static String firstNonBlank(String a, String fallback) {
        return (a == null || a.isBlank()) ? fallback : a;
    }

    private static LocalDateTime parseDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return LocalDateTime.now();
        try {
            return ZonedDateTime.parse(pubDate.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    /** Google News puts an HTML stub in the description; reduce it to plain text. */
    private static String stripHtml(String html) {
        if (html == null) return null;
        String text = html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
        return text.isEmpty() ? null : text;
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() <= max ? v : v.substring(0, max);
    }
}

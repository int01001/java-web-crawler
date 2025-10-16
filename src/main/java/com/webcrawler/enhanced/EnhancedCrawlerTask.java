package com.webcrawler.enhanced;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.webcrawler.CrawlerConfig;
import com.webcrawler.CrawlerStats;

public class EnhancedCrawlerTask implements Runnable {
    private final String url;
    private final int depth;
    private final CrawlerConfig config;
    private final BlockingQueue<EnhancedCrawlerTask> taskQueue;
    private final ConcurrentHashMap<String, Integer> visitedUrls;
    private final CrawlerStats stats;
    private final String baseDomain;
    private final DataExporter dataExporter;

    // Regular expressions for data extraction
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})\\b|" +
        "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"
    );

    public EnhancedCrawlerTask(String url, int depth, CrawlerConfig config,
                              BlockingQueue<EnhancedCrawlerTask> taskQueue,
                              ConcurrentHashMap<String, Integer> visitedUrls,
                              CrawlerStats stats, String baseDomain,
                              DataExporter dataExporter) {
        this.url = url;
        this.depth = depth;
        this.config = config;
        this.taskQueue = taskQueue;
        this.visitedUrls = visitedUrls;
        this.stats = stats;
        this.baseDomain = baseDomain;
        this.dataExporter = dataExporter;
    }

    @Override
    public void run() {
        try {
            // Add delay to be polite
            if (config.getDelayBetweenRequests() > 0) {
                Thread.sleep(config.getDelayBetweenRequests());
            }

            // Fetch and parse the page
            Document document = Jsoup.connect(url)
                .userAgent(config.getUserAgent())
                .timeout(config.getConnectTimeout())
                .get();

            System.out.println(String.format("🕷️ [Thread-%d] Crawled (depth %d): %s", 
                Thread.currentThread().getId(), depth, url));

            // Extract comprehensive data from the page
            PageData pageData = extractPageData(document);
            pageData.setDepth(depth);

            // Save content and add to exporter
            dataExporter.savePageContent(pageData, document.html());
            dataExporter.addPageData(pageData);

            // Update statistics
            stats.incrementPagesCrawled();
            stats.addBytesDownloaded(document.html().length());

            // Extract and queue new links if within depth limit
            if (depth < config.getMaxDepth()) {
                extractAndQueueLinks(document);
            }

            // Check for broken links
            checkLinksOnPage(document);

        } catch (IOException e) {
            System.err.println(String.format("❌ Failed to crawl %s: %s", url, e.getMessage()));
            stats.incrementFailedPages();
            dataExporter.addBrokenLink(url + " - " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Crawler task interrupted");
        }
    }

    private PageData extractPageData(Document document) {
        PageData pageData = new PageData(url);

        // Extract basic information
        pageData.setTitle(document.select("title").text());
        pageData.setContent(document.text());
        
        // Extract meta description
        String description = document.select("meta[name=description]").attr("content");
        if (description.isEmpty()) {
            description = document.select("meta[property=og:description]").attr("content");
        }
        pageData.setDescription(description);

        // Extract headings
        Elements headings = document.select("h1, h2, h3, h4, h5, h6");
        for (Element heading : headings) {
            String text = heading.text().trim();
            if (!text.isEmpty()) {
                pageData.addHeading(heading.tagName() + ": " + text);
            }
        }

        // Extract links
        Elements links = document.select("a[href]");
        pageData.setLinkCount(links.size());
        for (Element link : links) {
            String href = link.absUrl("href");
            if (!href.isEmpty()) {
                pageData.addLink(href);
            }
        }

        // Extract images
        Elements images = document.select("img[src]");
        pageData.setImageCount(images.size());
        for (Element img : images) {
            String src = img.absUrl("src");
            if (!src.isEmpty()) {
                pageData.addImage(src);
            }
        }

        // Extract emails
        String pageText = document.text();
        Matcher emailMatcher = EMAIL_PATTERN.matcher(pageText);
        while (emailMatcher.find()) {
            String email = emailMatcher.group().toLowerCase();
            pageData.addEmail(email);
        }

        // Extract phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(pageText);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group();
            pageData.addPhoneNumber(phone);
        }

        // Check for contact forms
        boolean hasContactForm = !document.select("form").isEmpty() && 
            (pageText.toLowerCase().contains("contact") ||
             pageText.toLowerCase().contains("email") ||
             document.select("input[type=email]").size() > 0 ||
             document.select("textarea").size() > 0);
        pageData.setHasContactForm(hasContactForm);

        return pageData;
    }

    private void extractAndQueueLinks(Document document) {
        Elements links = document.select("a[href]");
        Set<String> newUrls = new HashSet<>();

        for (Element link : links) {
            String href = link.absUrl("href");
            
            if (isValidUrl(href)) {
                newUrls.add(href);
            }
        }

        // Queue new URLs
        for (String newUrl : newUrls) {
            if (stats.getPagesCrawled() + stats.getPagesQueued() >= config.getMaxPages()) {
                break;
            }

            // Use putIfAbsent to atomically check and add
            if (visitedUrls.putIfAbsent(newUrl, depth + 1) == null) {
                try {
                    EnhancedCrawlerTask newTask = new EnhancedCrawlerTask(newUrl, depth + 1, config, 
                        taskQueue, visitedUrls, stats, baseDomain, dataExporter);
                    taskQueue.offer(newTask);
                    stats.incrementPagesQueued();
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to queue URL: " + newUrl);
                }
            }
        }
    }

    private void checkLinksOnPage(Document document) {
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String href = link.absUrl("href");
            if (!href.isEmpty() && !isLinkWorking(href)) {
                dataExporter.addBrokenLink(href + " (found on: " + url + ")");
            }
        }
    }

    private boolean isLinkWorking(String linkUrl) {
        try {
            // Only check HTTP/HTTPS links and do quick head request
            if (!linkUrl.startsWith("http")) return true;
            
            URL url = new URL(linkUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false; // Assume broken if we can't check
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol().toLowerCase();
            
            // Only HTTP and HTTPS
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                return false;
            }

            // Stay within the same domain (configurable restriction)
            String host = urlObj.getHost().toLowerCase();
            return host.equals(baseDomain) || host.endsWith("." + baseDomain);

        } catch (Exception e) {
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }
}
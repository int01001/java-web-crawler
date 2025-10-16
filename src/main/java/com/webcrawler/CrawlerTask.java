package com.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerTask implements Runnable {
    private final String url;
    private final int depth;
    private final CrawlerConfig config;
    private final BlockingQueue<CrawlerTask> taskQueue;
    private final ConcurrentHashMap<String, Integer> visitedUrls;
    private final CrawlerStats stats;
    private final String baseDomain;

    // Regular expressions for extracting useful data
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})\\b|" +
        "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"
    );

    public CrawlerTask(String url, int depth, CrawlerConfig config,
                      BlockingQueue<CrawlerTask> taskQueue,
                      ConcurrentHashMap<String, Integer> visitedUrls,
                      CrawlerStats stats, String baseDomain) {
        this.url = url;
        this.depth = depth;
        this.config = config;
        this.taskQueue = taskQueue;
        this.visitedUrls = visitedUrls;
        this.stats = stats;
        this.baseDomain = baseDomain;
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

            // Save the page content to files
            savePageContent(document, url);
            
            // Extract and save additional data
            extractAndSaveData(document, url);

            System.out.println(String.format("🕷️ [Thread-%d] Crawled (depth %d): %s", 
                Thread.currentThread().getId(), depth, url));

            // Update statistics
            stats.incrementPagesCrawled();
            stats.addBytesDownloaded(document.html().length());

            // Extract links if we haven't reached max depth
            if (depth < config.getMaxDepth()) {
                extractAndQueueLinks(document);
            }

        } catch (IOException e) {
            System.err.println(String.format("❌ Failed to crawl %s: %s", url, e.getMessage()));
            stats.incrementFailedPages();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Crawler task interrupted");
        }
    }

    private void savePageContent(Document document, String url) {
        try {
            // Create directories if they don't exist
            new File("crawler_output").mkdirs();
            new File("crawler_output/pages").mkdirs();
            new File("crawler_output/content").mkdirs();
            new File("crawler_output/data").mkdirs();
            
            // Create safe filename from URL
            String fileName = sanitizeFileName(url);
            
            // Save HTML content
            String htmlFile = "crawler_output/pages/" + fileName + ".html";
            Files.write(Paths.get(htmlFile), document.html().getBytes());
            
            // Save text content  
            String textFile = "crawler_output/content/" + fileName + ".txt";
            Files.write(Paths.get(textFile), document.text().getBytes());
            
            System.out.println("💾 Saved: " + fileName + " (HTML & Text)");
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save content for: " + url + " - " + e.getMessage());
        }
    }

    private void extractAndSaveData(Document document, String url) {
        try {
            // Extract page metadata
            String title = document.select("title").text();
            String description = document.select("meta[name=description]").attr("content");
            if (description.isEmpty()) {
                description = document.select("meta[property=og:description]").attr("content");
            }
            
            // Count elements
            int linkCount = document.select("a[href]").size();
            int imageCount = document.select("img[src]").size();
            int headingCount = document.select("h1, h2, h3, h4, h5, h6").size();
            int wordCount = document.text().split("\\s+").length;
            
            // Extract emails
            Set<String> emails = new HashSet<>();
            String pageText = document.text();
            Matcher emailMatcher = EMAIL_PATTERN.matcher(pageText);
            while (emailMatcher.find()) {
                emails.add(emailMatcher.group().toLowerCase());
            }
            
            // Extract phone numbers
            Set<String> phoneNumbers = new HashSet<>();
            Matcher phoneMatcher = PHONE_PATTERN.matcher(pageText);
            while (phoneMatcher.find()) {
                phoneNumbers.add(phoneMatcher.group());
            }
            
            // Check for contact forms
            boolean hasContactForm = !document.select("form").isEmpty() && 
                (pageText.toLowerCase().contains("contact") ||
                 pageText.toLowerCase().contains("email") ||
                 document.select("input[type=email]").size() > 0 ||
                 document.select("textarea").size() > 0);
            
            // Save extracted data to CSV format
            saveExtractedDataToCSV(url, title, description, linkCount, imageCount, 
                                 headingCount, wordCount, emails, phoneNumbers, hasContactForm);
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to extract data from: " + url + " - " + e.getMessage());
        }
    }

    private void saveExtractedDataToCSV(String url, String title, String description, 
                                       int linkCount, int imageCount, int headingCount, 
                                       int wordCount, Set<String> emails, Set<String> phoneNumbers, 
                                       boolean hasContactForm) {
        try {
            String csvFile = "crawler_output/data/crawl_data.csv";
            
            // Create CSV header if file doesn't exist
            File file = new File(csvFile);
            boolean writeHeader = !file.exists();
            
            StringBuilder csvData = new StringBuilder();
            
            if (writeHeader) {
                csvData.append("URL,Title,Description,Word_Count,Link_Count,Image_Count,Heading_Count,Email_Count,Phone_Count,Emails,Phone_Numbers,Has_Contact_Form,Crawl_Time,Depth\n");
            }
            
            // Prepare data row
            String escapedUrl = escapeCSV(url);
            String escapedTitle = escapeCSV(title);
            String escapedDescription = escapeCSV(description);
            String emailList = String.join("; ", emails);
            String phoneList = String.join("; ", phoneNumbers);
            String timestamp = new java.util.Date().toString();
            
            csvData.append(String.format("\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,%d,%d,\"%s\",\"%s\",%b,\"%s\",%d\n",
                escapedUrl, escapedTitle, escapedDescription, wordCount, linkCount, imageCount,
                headingCount, emails.size(), phoneNumbers.size(), emailList, phoneList,
                hasContactForm, timestamp, depth));
            
            // Append to CSV file
            Files.write(Paths.get(csvFile), csvData.toString().getBytes(), 
                       java.nio.file.StandardOpenOption.CREATE, 
                       java.nio.file.StandardOpenOption.APPEND);
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save CSV data for: " + url);
        }
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
                    CrawlerTask newTask = new CrawlerTask(newUrl, depth + 1, config, 
                        taskQueue, visitedUrls, stats, baseDomain);
                    taskQueue.offer(newTask);
                    stats.incrementPagesQueued();
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to queue URL: " + newUrl);
                }
            }
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

            // Stay within the same domain (optional restriction)
            String host = urlObj.getHost().toLowerCase();
            return host.equals(baseDomain) || host.endsWith("." + baseDomain);

        } catch (Exception e) {
            return false;
        }
    }

    private String sanitizeFileName(String url) {
        try {
            URL urlObj = new URL(url);
            String fileName = urlObj.getHost() + urlObj.getPath();
            return fileName.replaceAll("[^a-zA-Z0-9.-]", "_")
                          .replaceAll("_{2,}", "_")
                          .replaceAll("^_|_$", "");
        } catch (Exception e) {
            return url.replaceAll("[^a-zA-Z0-9.-]", "_")
                     .replaceAll("_{2,}", "_")
                     .replaceAll("^_|_$", "");
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }
}

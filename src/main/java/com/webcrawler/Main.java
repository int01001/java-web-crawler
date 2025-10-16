package com.webcrawler;

public class Main {
    public static void main(String[] args) {
        // Configure the crawler
        CrawlerConfig config = new CrawlerConfig.Builder()
            .maxThreads(10)
            .maxPages(50)
            .maxDepth(3)
            .delayBetweenRequests(1000) // 1 second delay
            .connectTimeout(10000) // 10 seconds
            .build();

        // Create and start the crawler
        WebCrawler crawler = new WebCrawler(config);
        
        // Start crawling from seed URL
        String seedUrl = "https://github.com/trending"; // Change to your target URL
        
        try {
            System.out.println("Starting web crawler...");
            crawler.startCrawling(seedUrl);
        } catch (Exception e) {
            System.err.println("Crawler failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
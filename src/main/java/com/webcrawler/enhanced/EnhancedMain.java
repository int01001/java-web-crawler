package com.webcrawler.enhanced;

import com.webcrawler.CrawlerConfig;
import com.webcrawler.CrawlerStats;

public class EnhancedMain {
    public static void main(String[] args) {
        // Enhanced configuration with more features
        CrawlerConfig config = new CrawlerConfig.Builder()
            .maxThreads(15)
            .maxPages(100)
            .maxDepth(4)
            .delayBetweenRequests(800) // Slightly faster for testing
            .connectTimeout(15000) // 15 seconds
            .build();

        // Create enhanced crawler with all features
        EnhancedWebCrawler crawler = new EnhancedWebCrawler(config);
        
        // Start crawling - Change this URL to test different sites
        String seedUrl = "https://www.srmist.edu.in/"; 
        // Try these for more interesting results:
        // String seedUrl = "https://stackoverflow.com";
        // String seedUrl = "https://news.ycombinator.com"; 
        // String seedUrl = "https://en.wikipedia.org/wiki/Main_Page";
        
        try {
            System.out.println("🕷️  Starting Enhanced Web Crawler...");
            System.out.println("📁 Results will be saved to 'crawler_output' folder");
            System.out.println("=" + "=".repeat(60));
            
            crawler.startCrawling(seedUrl);
            
            System.out.println("=" + "=".repeat(60));
            System.out.println("✅ Crawling completed successfully!");
            System.out.println("📊 Check the crawler_output folder for results");
            
        } catch (Exception e) {
            System.err.println("❌ Crawler failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
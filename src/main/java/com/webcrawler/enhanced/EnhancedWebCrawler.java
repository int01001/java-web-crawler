package com.webcrawler.enhanced;

import java.net.URL;
import java.util.concurrent.*;

import com.webcrawler.CrawlerConfig;
import com.webcrawler.CrawlerStats;

public class EnhancedWebCrawler {
    private final CrawlerConfig config;
    private final ExecutorService executor;
    private final BlockingQueue<EnhancedCrawlerTask> taskQueue;
    private final ConcurrentHashMap<String, Integer> visitedUrls;
    private final CrawlerStats stats;
    private final DataExporter dataExporter;

    public EnhancedWebCrawler(CrawlerConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.getMaxThreads());
        this.taskQueue = new LinkedBlockingQueue<>();
        this.visitedUrls = new ConcurrentHashMap<>();
        this.stats = new CrawlerStats();
        this.dataExporter = new DataExporter("crawler_output");
    }

    public void startCrawling(String seedUrl) throws InterruptedException {
        String baseDomain = extractDomain(seedUrl);
        if (baseDomain == null) {
            throw new IllegalArgumentException("Invalid seed URL: " + seedUrl);
        }

        System.out.println("🚀 Starting Enhanced Web Crawler with " + config.getMaxThreads() + " threads");
        System.out.println("🎯 Target domain: " + baseDomain);
        System.out.println("📄 Max pages: " + config.getMaxPages());
        System.out.println("🔍 Max depth: " + config.getMaxDepth());
        System.out.println("⏱️  Delay between requests: " + config.getDelayBetweenRequests() + "ms");
        System.out.println();

        // Add seed URL to the queue
        EnhancedCrawlerTask seedTask = new EnhancedCrawlerTask(seedUrl, 0, config, taskQueue, 
            visitedUrls, stats, baseDomain, dataExporter);
        taskQueue.offer(seedTask);
        visitedUrls.put(seedUrl, 0);
        stats.incrementPagesQueued();

        // Start worker threads
        for (int i = 0; i < config.getMaxThreads(); i++) {
            executor.submit(new EnhancedCrawlerWorker());
        }

        // Monitor progress
        monitorProgress();

        // Export all data and shutdown
        dataExporter.exportAllData();
        shutdown();
    }

    private void monitorProgress() throws InterruptedException {
        int progressCounter = 0;
        while (true) {
            Thread.sleep(5000); // Print stats every 5 seconds

            stats.printStats();
            progressCounter++;

            // Show additional stats every 3rd update (15 seconds)
            if (progressCounter % 3 == 0) {
                printEnhancedStats();
            }

            // Check stopping conditions
            if (stats.getPagesCrawled() >= config.getMaxPages()) {
                System.out.println("🛑 Reached maximum pages limit");
                break;
            }

            if (taskQueue.isEmpty() && allThreadsIdle()) {
                System.out.println("✅ No more URLs to crawl");
                break;
            }
        }
    }

    private void printEnhancedStats() {
        System.out.println("📊 Enhanced Stats - " +
            "Domains: " + dataExporter.getTotalDomains() + ", " +
            "Emails: " + dataExporter.getTotalEmails() + ", " +
            "Phones: " + dataExporter.getTotalPhoneNumbers() + ", " +
            "Broken Links: " + dataExporter.getTotalBrokenLinks());
    }

    private boolean allThreadsIdle() {
        // Simple heuristic: if queue is empty for a few seconds, assume threads are idle
        try {
            Thread.sleep(2000);
            return taskQueue.isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void shutdown() {
        System.out.println("\n🔄 Shutting down crawler...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("⚠️ Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("\n🎉 === FINAL CRAWL RESULTS ===");
        stats.printStats();
        System.out.println("📊 Enhanced Results:");
        System.out.println("   - Unique URLs discovered: " + visitedUrls.size());
        System.out.println("   - Domains crawled: " + dataExporter.getTotalDomains());
        System.out.println("   - Email addresses found: " + dataExporter.getTotalEmails());
        System.out.println("   - Phone numbers found: " + dataExporter.getTotalPhoneNumbers());
        System.out.println("   - Broken links detected: " + dataExporter.getTotalBrokenLinks());
        System.out.println("\n📁 All results saved to 'crawler_output' folder");
        System.out.println("   📄 HTML pages: crawler_output/pages/");
        System.out.println("   📝 Text content: crawler_output/content/");
        System.out.println("   📊 Data exports: crawler_output/data/");
        System.out.println("   📋 Reports: crawler_output/reports/");
    }

    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    // Inner class for worker threads
    private class EnhancedCrawlerWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    EnhancedCrawlerTask task = taskQueue.poll(5, TimeUnit.SECONDS);
                    if (task == null) {
                        continue; // Timeout, check again
                    }

                    // Check if we've reached the limit
                    if (stats.getPagesCrawled() >= config.getMaxPages()) {
                        taskQueue.offer(task); // Put it back
                        break;
                    }

                    task.run();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ Worker thread error: " + e.getMessage());
                }
            }
        }
    }
}
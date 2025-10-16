# Multithreaded Web Crawler Project in Java

A complete, easy-to-understand multithreaded web crawler implementation that demonstrates Java concurrency concepts, thread-safe data structures, and web scraping.

## Project Structure

```
WebCrawler/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── webcrawler/
│                   ├── WebCrawler.java          # Main crawler class
│                   ├── CrawlerTask.java         # Individual crawler task (Runnable)
│                   ├── CrawlerConfig.java       # Configuration settings
│                   ├── CrawlerStats.java        # Statistics tracking
│                   └── Main.java                # Entry point
├── pom.xml                                      # Maven dependencies
└── README.md                                    # Project documentation
```

## Dependencies Required

### Maven (pom.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.webcrawler</groupId>
    <artifactId>multithreaded-web-crawler</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.17.1</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Core Classes

### 1. Main.java - Entry Point
```java
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
        String seedUrl = "https://example.com"; // Change to your target URL
        
        try {
            System.out.println("Starting web crawler...");
            crawler.startCrawling(seedUrl);
        } catch (Exception e) {
            System.err.println("Crawler failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 2. CrawlerConfig.java - Configuration Management
```java
package com.webcrawler;

public class CrawlerConfig {
    private final int maxThreads;
    private final int maxPages;
    private final int maxDepth;
    private final long delayBetweenRequests;
    private final int connectTimeout;
    private final String userAgent;

    private CrawlerConfig(Builder builder) {
        this.maxThreads = builder.maxThreads;
        this.maxPages = builder.maxPages;
        this.maxDepth = builder.maxDepth;
        this.delayBetweenRequests = builder.delayBetweenRequests;
        this.connectTimeout = builder.connectTimeout;
        this.userAgent = builder.userAgent;
    }

    // Getters
    public int getMaxThreads() { return maxThreads; }
    public int getMaxPages() { return maxPages; }
    public int getMaxDepth() { return maxDepth; }
    public long getDelayBetweenRequests() { return delayBetweenRequests; }
    public int getConnectTimeout() { return connectTimeout; }
    public String getUserAgent() { return userAgent; }

    public static class Builder {
        private int maxThreads = 5;
        private int maxPages = 100;
        private int maxDepth = 2;
        private long delayBetweenRequests = 1000;
        private int connectTimeout = 10000;
        private String userAgent = "WebCrawler/1.0";

        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public Builder maxPages(int maxPages) {
            this.maxPages = maxPages;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder delayBetweenRequests(long delay) {
            this.delayBetweenRequests = delay;
            return this;
        }

        public Builder connectTimeout(int timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public CrawlerConfig build() {
            return new CrawlerConfig(this);
        }
    }
}
```

### 3. CrawlerStats.java - Thread-Safe Statistics
```java
package com.webcrawler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CrawlerStats {
    private final AtomicInteger pagesCrawled = new AtomicInteger(0);
    private final AtomicInteger pagesQueued = new AtomicInteger(0);
    private final AtomicInteger failedPages = new AtomicInteger(0);
    private final AtomicLong totalBytesDownloaded = new AtomicLong(0);
    private final long startTime;

    public CrawlerStats() {
        this.startTime = System.currentTimeMillis();
    }

    public void incrementPagesCrawled() {
        pagesCrawled.incrementAndGet();
    }

    public void incrementPagesQueued() {
        pagesQueued.incrementAndGet();
    }

    public void incrementFailedPages() {
        failedPages.incrementAndGet();
    }

    public void addBytesDownloaded(long bytes) {
        totalBytesDownloaded.addAndGet(bytes);
    }

    public int getPagesCrawled() {
        return pagesCrawled.get();
    }

    public int getPagesQueued() {
        return pagesQueued.get();
    }

    public int getFailedPages() {
        return failedPages.get();
    }

    public long getTotalBytesDownloaded() {
        return totalBytesDownloaded.get();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public double getPagesPerSecond() {
        long elapsed = getElapsedTime();
        return elapsed > 0 ? (double) pagesCrawled.get() / (elapsed / 1000.0) : 0;
    }

    public void printStats() {
        System.out.printf("Stats - Crawled: %d, Queued: %d, Failed: %d, Bytes: %.2f MB, Speed: %.2f pages/sec%n",
            getPagesCrawled(),
            getPagesQueued(),
            getFailedPages(),
            getTotalBytesDownloaded() / (1024.0 * 1024.0),
            getPagesPerSecond()
        );
    }
}
```

### 4. CrawlerTask.java - Individual Crawler Thread
```java
package com.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class CrawlerTask implements Runnable {
    private final String url;
    private final int depth;
    private final CrawlerConfig config;
    private final BlockingQueue<CrawlerTask> taskQueue;
    private final ConcurrentHashMap<String, Integer> visitedUrls;
    private final CrawlerStats stats;
    private final String baseDomain;

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

            System.out.println(String.format("[Thread-%d] Crawled (depth %d): %s", 
                Thread.currentThread().getId(), depth, url));

            // Update statistics
            stats.incrementPagesCrawled();
            stats.addBytesDownloaded(document.html().length());

            // Extract links if we haven't reached max depth
            if (depth < config.getMaxDepth()) {
                extractAndQueueLinks(document);
            }

        } catch (IOException e) {
            System.err.println(String.format("Failed to crawl %s: %s", url, e.getMessage()));
            stats.incrementFailedPages();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Crawler task interrupted");
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
                    System.err.println("Failed to queue URL: " + newUrl);
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

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }
}
```

### 5. WebCrawler.java - Main Crawler Class
```java
package com.webcrawler;

import java.net.URL;
import java.util.concurrent.*;

public class WebCrawler {
    private final CrawlerConfig config;
    private final ExecutorService executor;
    private final BlockingQueue<CrawlerTask> taskQueue;
    private final ConcurrentHashMap<String, Integer> visitedUrls;
    private final CrawlerStats stats;

    public WebCrawler(CrawlerConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.getMaxThreads());
        this.taskQueue = new LinkedBlockingQueue<>();
        this.visitedUrls = new ConcurrentHashMap<>();
        this.stats = new CrawlerStats();
    }

    public void startCrawling(String seedUrl) throws InterruptedException {
        String baseDomain = extractDomain(seedUrl);
        if (baseDomain == null) {
            throw new IllegalArgumentException("Invalid seed URL: " + seedUrl);
        }

        System.out.println("Starting crawler with " + config.getMaxThreads() + " threads");
        System.out.println("Target domain: " + baseDomain);
        System.out.println("Max pages: " + config.getMaxPages());
        System.out.println("Max depth: " + config.getMaxDepth());

        // Add seed URL to the queue
        CrawlerTask seedTask = new CrawlerTask(seedUrl, 0, config, taskQueue, 
            visitedUrls, stats, baseDomain);
        taskQueue.offer(seedTask);
        visitedUrls.put(seedUrl, 0);
        stats.incrementPagesQueued();

        // Start worker threads
        for (int i = 0; i < config.getMaxThreads(); i++) {
            executor.submit(new CrawlerWorker());
        }

        // Monitor progress
        monitorProgress();

        // Shutdown
        shutdown();
    }

    private void monitorProgress() throws InterruptedException {
        while (true) {
            Thread.sleep(5000); // Print stats every 5 seconds

            stats.printStats();

            // Check stopping conditions
            if (stats.getPagesCrawled() >= config.getMaxPages()) {
                System.out.println("Reached maximum pages limit");
                break;
            }

            if (taskQueue.isEmpty() && allThreadsIdle()) {
                System.out.println("No more URLs to crawl");
                break;
            }
        }
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
        System.out.println("Shutting down crawler...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("\n=== FINAL STATISTICS ===");
        stats.printStats();
        System.out.println("Unique URLs discovered: " + visitedUrls.size());
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
    private class CrawlerWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CrawlerTask task = taskQueue.poll(5, TimeUnit.SECONDS);
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
                    System.err.println("Worker thread error: " + e.getMessage());
                }
            }
        }
    }
}
```

## How to Run

1. **Setup Project:**
   ```bash
   mkdir WebCrawler
   cd WebCrawler
   # Create the directory structure and files above
   ```

2. **Compile and Run with Maven:**
   ```bash
   mvn clean compile
   mvn exec:java -Dexec.mainClass="com.webcrawler.Main"
   ```

3. **Or compile manually:**
   ```bash
   javac -cp "jsoup-1.17.1.jar" src/main/java/com/webcrawler/*.java
   java -cp ".:jsoup-1.17.1.jar:src/main/java" com.webcrawler.Main
   ```

## Key Features Demonstrated

### 1. **Java Concurrency:**
- `ExecutorService` with fixed thread pool
- Custom `Runnable` tasks (`CrawlerTask`)
- Proper thread shutdown and resource cleanup

### 2. **Thread-Safe Data Structures:**
- `ConcurrentHashMap` for visited URLs
- `LinkedBlockingQueue` for task management  
- `AtomicInteger` and `AtomicLong` for statistics

### 3. **Resource Management:**
- Connection timeouts and proper error handling
- Controlled request rate with delays
- Memory-efficient URL processing

### 4. **Web Scraping:**
- Jsoup for HTML fetching and parsing
- Link extraction with absolute URL resolution
- Domain-based filtering

## Customization Options

- **Change target website:** Modify `seedUrl` in `Main.java`
- **Adjust thread count:** Use `maxThreads()` in config
- **Set crawling limits:** Configure `maxPages()` and `maxDepth()`
- **Control politeness:** Adjust `delayBetweenRequests()`
- **Domain restrictions:** Modify `isValidUrl()` method

This project provides a solid foundation for understanding multithreaded web crawling while being simple enough for educational purposes and extensible for real-world applications.
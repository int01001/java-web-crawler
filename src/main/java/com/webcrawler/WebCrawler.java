package com.webcrawler;

import java.net.URL;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebCrawler {
    private final CrawlerConfig config;
    private final ExecutorService executor;
    private final BlockingQueue<CrawlerTask> taskQueue;
    private final ConcurrentHashMap<String, Integer> visitedUrls;
    private final CrawlerStats stats;

    private Consumer<String> logConsumer;
    private List<CrawlStatusListener> listeners = new CopyOnWriteArrayList<>();

    public WebCrawler(CrawlerConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.getMaxThreads());
        this.taskQueue = new LinkedBlockingQueue<>();
        this.visitedUrls = new ConcurrentHashMap<>();
        this.stats = new CrawlerStats();
    }

    // GUI logging support
    public void setLogConsumer(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    private void printLog(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        } else {
            System.out.println(message);
        }
    }

    // CrawlStatusListener (optional, for advanced GUIs)
    public interface CrawlStatusListener {
        void onStatusUpdate(CrawlerStats stats);
    }
    public void addStatusListener(CrawlStatusListener listener) {
        listeners.add(listener);
    }
    private void notifyStatusListeners() {
        for (CrawlStatusListener listener : listeners) {
            listener.onStatusUpdate(stats);
        }
    }

    public void startCrawling(String seedUrl) throws InterruptedException {
        String baseDomain = extractDomain(seedUrl);
        if (baseDomain == null) {
            throw new IllegalArgumentException("Invalid seed URL: " + seedUrl);
        }

        printLog("Starting crawler with " + config.getMaxThreads() + " threads");
        printLog("Target domain: " + baseDomain);
        printLog("Max pages: " + config.getMaxPages());
        printLog("Max depth: " + config.getMaxDepth());

        CrawlerTask seedTask = new CrawlerTask(seedUrl, 0, config, taskQueue, visitedUrls, stats, baseDomain);
        taskQueue.offer(seedTask);
        visitedUrls.put(seedUrl, 0);
        stats.incrementPagesQueued();

        for (int i = 0; i < config.getMaxThreads(); i++) {
            executor.submit(new CrawlerWorker());
        }

        monitorProgress();
        shutdown();
    }

    private void monitorProgress() throws InterruptedException {
        int progressCounter = 0;
        while (true) {
            Thread.sleep(5000);

            printLog(String.format("Stats - Crawled: %d, Queued: %d, Failed: %d, Bytes: %.2f MB, Speed: %.2f pages/sec",
                    stats.getPagesCrawled(), stats.getPagesQueued(), stats.getFailedPages(),
                    stats.getTotalBytesDownloaded() / (1024.0 * 1024.0), stats.getPagesPerSecond()));

            progressCounter++;
            if (progressCounter % 3 == 0) {
                notifyStatusListeners();
            }

            if (stats.getPagesCrawled() >= config.getMaxPages()) {
                printLog("Reached maximum pages limit");
                break;
            }
            if (taskQueue.isEmpty() && allThreadsIdle()) {
                printLog("No more URLs to crawl");
                break;
            }
        }
    }

    private boolean allThreadsIdle() {
        try {
            Thread.sleep(2000);
            return taskQueue.isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void shutdown() {
        printLog("\nShutting down crawler...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    printLog("Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        printLog("\n=== FINAL STATISTICS ===");
        printLog(String.format("Stats - Crawled: %d, Queued: %d, Failed: %d, Bytes: %.2f MB, Speed: %.2f pages/sec",
                stats.getPagesCrawled(), stats.getPagesQueued(), stats.getFailedPages(),
                stats.getTotalBytesDownloaded() / (1024.0 * 1024.0), stats.getPagesPerSecond()));

        printLog("Unique URLs discovered: " + visitedUrls.size());
    }

    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private class CrawlerWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CrawlerTask task = taskQueue.poll(5, TimeUnit.SECONDS);
                    if (task == null) continue;
                    if (stats.getPagesCrawled() >= config.getMaxPages()) {
                        taskQueue.offer(task);
                        break;
                    }
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    printLog("Worker thread error: " + e.getMessage());
                }
            }
        }
    }
}

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
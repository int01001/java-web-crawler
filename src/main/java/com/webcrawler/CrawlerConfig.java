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
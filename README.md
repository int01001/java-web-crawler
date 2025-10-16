# Multithreaded Web Crawler in Java

A complete, production-ready multithreaded web crawler that demonstrates advanced Java concurrency concepts, thread-safe data structures, and efficient web scraping.

## 🚀 Quick Start

1. **Clone or download the project files**
2. **Build with Maven:**
   ```bash
   mvn clean compile
   ```
3. **Run the crawler:**
   ```bash
   mvn exec:java -Dexec.mainClass="com.webcrawler.Main"
   ```

## 📁 Project Structure

```
WebCrawler/
├── src/main/java/com/webcrawler/
│   ├── Main.java              # Entry point and configuration
│   ├── WebCrawler.java        # Core crawler with ExecutorService
│   ├── CrawlerTask.java       # Runnable task for individual page crawling
│   ├── CrawlerConfig.java     # Configuration with Builder pattern
│   └── CrawlerStats.java      # Thread-safe statistics tracking
├── pom.xml                    # Maven dependencies and build configuration
└── README.md                  # This file
```

## 🔧 Configuration

Modify the configuration in `Main.java`:

```java
CrawlerConfig config = new CrawlerConfig.Builder()
    .maxThreads(10)                    // Number of concurrent threads
    .maxPages(50)                      // Maximum pages to crawl
    .maxDepth(3)                       // Maximum link depth
    .delayBetweenRequests(1000)        // Politeness delay (ms)
    .connectTimeout(10000)             // Connection timeout (ms)
    .build();

String seedUrl = "https://example.com"; // Change this to your target
```

## 🎯 Key Features Demonstrated

### Java Concurrency
- **ExecutorService** with fixed thread pool management
- **Runnable** interface implementation for crawler tasks
- Proper thread lifecycle management and graceful shutdown

### Thread-Safe Data Structures
- **ConcurrentHashMap** for tracking visited URLs
- **LinkedBlockingQueue** for task queue management
- **AtomicInteger/AtomicLong** for thread-safe statistics

### Resource Management
- Connection timeouts and error handling
- Controlled request rate with politeness delays
- Efficient memory usage with proper cleanup

### Web Scraping
- **Jsoup** library for HTML parsing and link extraction
- Absolute URL resolution
- Domain-based URL filtering
- User-Agent and timeout configuration

## 📊 Sample Output

```
Starting crawler with 10 threads
Target domain: example.com
Max pages: 50
Max depth: 3
[Thread-12] Crawled (depth 0): https://example.com
[Thread-13] Crawled (depth 1): https://example.com/about
Stats - Crawled: 15, Queued: 23, Failed: 2, Bytes: 1.25 MB, Speed: 3.2 pages/sec
...
=== FINAL STATISTICS ===
Stats - Crawled: 50, Queued: 67, Failed: 5, Bytes: 4.8 MB, Speed: 2.8 pages/sec
Unique URLs discovered: 125
```

## ⚙️ Advanced Customization

### Custom URL Filtering
Modify `isValidUrl()` in `CrawlerTask.java` to change URL filtering logic:
- Remove domain restrictions for broader crawling
- Add file extension filters
- Implement robots.txt respect

### Enhanced Statistics
Extend `CrawlerStats.java` to track:
- Response times
- HTTP status codes
- Content types
- Link depth distribution

### Persistence
Add database integration to store:
- Crawled pages and metadata
- URL queue for resumable crawling
- Crawl history and analytics

## 🛡️ Best Practices Implemented

- **Race Condition Prevention**: Using thread-safe collections
- **Resource Cleanup**: Proper ExecutorService shutdown
- **Politeness**: Configurable delays between requests
- **Error Handling**: Graceful failure management
- **Statistics**: Real-time progress monitoring
- **Domain Respect**: Optional same-domain crawling

## 📚 Learning Outcomes

This project teaches:
1. **Concurrent Programming**: Thread pools, synchronization
2. **Data Structure Design**: Thread-safe collection usage
3. **Network Programming**: HTTP client best practices
4. **Design Patterns**: Builder, Factory, Observer patterns
5. **Error Handling**: Robust exception management
6. **Resource Management**: Memory and connection efficiency

## 🚨 Important Notes

- **Respect robots.txt**: Add robots.txt parsing for production use
- **Rate Limiting**: Adjust delays based on target site requirements  
- **Legal Compliance**: Ensure crawling complies with terms of service
- **Memory Usage**: Monitor for large-scale crawling scenarios

## 🔗 Dependencies

- **Java 11+**: Modern language features
- **Jsoup 1.17.1**: HTML parsing and HTTP client
- **Maven**: Build and dependency management

Ready to explore web crawling and Java concurrency! 🕷️

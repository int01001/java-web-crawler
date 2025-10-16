package com.webcrawler.enhanced;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataExporter {
    private final String outputDir;
    private final Map<String, List<PageData>> dataByDomain;
    private final List<PageData> allPageData;
    private final List<String> brokenLinks;
    private final Set<String> allEmails;
    private final Set<String> allPhoneNumbers;

    public DataExporter(String outputDir) {
        this.outputDir = outputDir;
        this.dataByDomain = new ConcurrentHashMap<>();
        this.allPageData = Collections.synchronizedList(new ArrayList<>());
        this.brokenLinks = Collections.synchronizedList(new ArrayList<>());
        this.allEmails = Collections.synchronizedSet(new HashSet<>());
        this.allPhoneNumbers = Collections.synchronizedSet(new HashSet<>());
        
        // Create output directories
        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(outputDir));
            Files.createDirectories(Paths.get(outputDir, "pages"));
            Files.createDirectories(Paths.get(outputDir, "content"));
            Files.createDirectories(Paths.get(outputDir, "images"));
            Files.createDirectories(Paths.get(outputDir, "reports"));
            Files.createDirectories(Paths.get(outputDir, "data"));
        } catch (IOException e) {
            System.err.println("Failed to create output directories: " + e.getMessage());
        }
    }

    public void addPageData(PageData pageData) {
        allPageData.add(pageData);
        
        // Group by domain
        String domain = pageData.getDomain();
        dataByDomain.computeIfAbsent(domain, k -> Collections.synchronizedList(new ArrayList<>()))
                   .add(pageData);
        
        // Collect emails and phone numbers
        allEmails.addAll(pageData.getEmails());
        allPhoneNumbers.addAll(pageData.getPhoneNumbers());
    }

    public void addBrokenLink(String brokenLink) {
        brokenLinks.add(brokenLink);
    }

    public void savePageContent(PageData pageData, String htmlContent) {
        try {
            // Save HTML content
            String fileName = sanitizeFileName(pageData.getUrl()) + ".html";
            Path htmlFile = Paths.get(outputDir, "pages", fileName);
            Files.write(htmlFile, htmlContent.getBytes());

            // Save text content
            String textFileName = sanitizeFileName(pageData.getUrl()) + ".txt";
            Path textFile = Paths.get(outputDir, "content", textFileName);
            Files.write(textFile, pageData.getContent().getBytes());

            System.out.println("💾 Saved: " + fileName);

        } catch (IOException e) {
            System.err.println("Failed to save content for: " + pageData.getUrl());
        }
    }

    public void exportAllData() {
        try {
            exportToCSV();
            exportToJSON();
            exportBrokenLinks();
            exportContactInfo();
            exportDomainAnalysis();
            exportSummaryReport();
            System.out.println("📊 All data exported successfully!");
        } catch (Exception e) {
            System.err.println("Failed to export data: " + e.getMessage());
        }
    }

    private void exportToCSV() throws IOException {
        Path csvFile = Paths.get(outputDir, "data", "crawl_results.csv");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvFile))) {
            // CSV Header
            writer.println("URL,Title,Description,Word_Count,Link_Count,Image_Count,Heading_Count,Email_Count,Phone_Count,Domain,Has_Contact_Form,Depth,Crawl_Time");
            
            // Data rows
            for (PageData data : allPageData) {
                writer.println(data.toCSV());
            }
        }
        System.out.println("📄 Exported CSV: " + csvFile);
    }

    private void exportToJSON() throws IOException {
        Path jsonFile = Paths.get(outputDir, "data", "crawl_results.json");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(jsonFile))) {
            writer.println("[\n");
            for (int i = 0; i < allPageData.size(); i++) {
                writer.print(allPageData.get(i).toJSON());
                if (i < allPageData.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            writer.println("]");
        }
        System.out.println("📄 Exported JSON: " + jsonFile);
    }

    private void exportBrokenLinks() throws IOException {
        if (brokenLinks.isEmpty()) return;

        Path brokenFile = Paths.get(outputDir, "reports", "broken_links.txt");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(brokenFile))) {
            writer.println("BROKEN LINKS REPORT");
            writer.println("==================");
            writer.println("Total broken links found: " + brokenLinks.size());
            writer.println();
            
            for (String link : brokenLinks) {
                writer.println(link);
            }
        }
        System.out.println("🔗 Exported broken links: " + brokenFile);
    }

    private void exportContactInfo() throws IOException {
        Path contactFile = Paths.get(outputDir, "data", "contact_info.txt");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(contactFile))) {
            writer.println("CONTACT INFORMATION FOUND");
            writer.println("========================");
            
            writer.println("EMAIL ADDRESSES (" + allEmails.size() + "):");
            writer.println("-".repeat(30));
            for (String email : allEmails) {
                writer.println(email);
            }
            
            writer.println("\nPHONE NUMBERS (" + allPhoneNumbers.size() + "):");
            writer.println("-".repeat(30));
            for (String phone : allPhoneNumbers) {
                writer.println(phone);
            }
        }
        System.out.println("📞 Exported contact info: " + contactFile);
    }

    private void exportDomainAnalysis() throws IOException {
        Path domainFile = Paths.get(outputDir, "reports", "domain_analysis.txt");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(domainFile))) {
            writer.println("DOMAIN ANALYSIS REPORT");
            writer.println("=====================");
            writer.println();
            
            for (Map.Entry<String, List<PageData>> entry : dataByDomain.entrySet()) {
                String domain = entry.getKey();
                List<PageData> pages = entry.getValue();
                
                writer.println("Domain: " + domain);
                writer.println("Pages crawled: " + pages.size());
                
                // Calculate averages
                double avgWords = pages.stream().mapToInt(PageData::getWordCount).average().orElse(0);
                double avgLinks = pages.stream().mapToInt(PageData::getLinkCount).average().orElse(0);
                double avgImages = pages.stream().mapToInt(PageData::getImageCount).average().orElse(0);
                
                writer.printf("Average word count: %.1f%n", avgWords);
                writer.printf("Average links per page: %.1f%n", avgLinks);
                writer.printf("Average images per page: %.1f%n", avgImages);
                
                // Count pages with contact forms
                long contactFormPages = pages.stream().filter(PageData::hasContactForm).count();
                writer.println("Pages with contact forms: " + contactFormPages);
                
                writer.println("-".repeat(50));
                writer.println();
            }
        }
        System.out.println("🌐 Exported domain analysis: " + domainFile);
    }

    private void exportSummaryReport() throws IOException {
        Path summaryFile = Paths.get(outputDir, "reports", "crawl_summary.txt");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(summaryFile))) {
            writer.println("WEB CRAWLER SUMMARY REPORT");
            writer.println("=" .repeat(50));
            writer.println("Generated: " + new Date());
            writer.println();
            
            // Overall statistics
            writer.println("OVERALL STATISTICS");
            writer.println("-".repeat(30));
            writer.println("Total pages crawled: " + allPageData.size());
            writer.println("Total domains: " + dataByDomain.size());
            writer.println("Total broken links: " + brokenLinks.size());
            writer.println("Total emails found: " + allEmails.size());
            writer.println("Total phone numbers found: " + allPhoneNumbers.size());
            writer.println();
            
            // Content statistics
            int totalWords = allPageData.stream().mapToInt(PageData::getWordCount).sum();
            int totalLinks = allPageData.stream().mapToInt(PageData::getLinkCount).sum();
            int totalImages = allPageData.stream().mapToInt(PageData::getImageCount).sum();
            long totalContactForms = allPageData.stream().filter(PageData::hasContactForm).count();
            
            writer.println("CONTENT STATISTICS");
            writer.println("-".repeat(30));
            writer.println("Total words: " + totalWords);
            writer.println("Total links found: " + totalLinks);
            writer.println("Total images found: " + totalImages);
            writer.println("Pages with contact forms: " + totalContactForms);
            writer.println();
            
            // Top domains by page count
            writer.println("TOP DOMAINS BY PAGE COUNT");
            writer.println("-".repeat(30));
            dataByDomain.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .limit(10)
                .forEach(entry -> writer.println(entry.getKey() + ": " + entry.getValue().size() + " pages"));
            
            writer.println();
            
            // Files created
            writer.println("FILES CREATED");
            writer.println("-".repeat(30));
            writer.println("- pages/: HTML files of crawled pages");
            writer.println("- content/: Text content extracted from pages");
            writer.println("- data/crawl_results.csv: Complete data in CSV format");
            writer.println("- data/crawl_results.json: Complete data in JSON format");
            writer.println("- data/contact_info.txt: All emails and phone numbers found");
            writer.println("- reports/broken_links.txt: List of broken links (if any)");
            writer.println("- reports/domain_analysis.txt: Analysis by domain");
            writer.println("- reports/crawl_summary.txt: This summary report");
        }
        System.out.println("📋 Exported summary report: " + summaryFile);
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

    public int getTotalPages() {
        return allPageData.size();
    }

    public int getTotalDomains() {
        return dataByDomain.size();
    }

    public int getTotalBrokenLinks() {
        return brokenLinks.size();
    }

    public int getTotalEmails() {
        return allEmails.size();
    }

    public int getTotalPhoneNumbers() {
        return allPhoneNumbers.size();
    }
}
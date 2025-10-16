package com.webcrawler.enhanced;

import java.util.List;
import java.util.ArrayList;

public class PageData {
    private String url;
    private String title;
    private String content;
    private int linkCount;
    private int imageCount;
    private int wordCount;
    private List<String> headings;
    private List<String> emails;
    private List<String> phoneNumbers;
    private List<String> links;
    private List<String> images;
    private boolean hasContactForm;
    private String description;
    private long contentLength;
    private long crawlTime;
    private int depth;
    private String domain;

    public PageData(String url) {
        this.url = url;
        this.headings = new ArrayList<>();
        this.emails = new ArrayList<>();
        this.phoneNumbers = new ArrayList<>();
        this.links = new ArrayList<>();
        this.images = new ArrayList<>();
        this.crawlTime = System.currentTimeMillis();
        
        // Extract domain from URL
        try {
            this.domain = new java.net.URL(url).getHost();
        } catch (Exception e) {
            this.domain = "unknown";
        }
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { 
        this.content = content;
        this.contentLength = content != null ? content.length() : 0;
        this.wordCount = content != null ? content.split("\\s+").length : 0;
    }

    public int getLinkCount() { return linkCount; }
    public void setLinkCount(int linkCount) { this.linkCount = linkCount; }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    public int getWordCount() { return wordCount; }

    public List<String> getHeadings() { return headings; }
    public void setHeadings(List<String> headings) { this.headings = headings; }
    public void addHeading(String heading) { this.headings.add(heading); }

    public List<String> getEmails() { return emails; }
    public void setEmails(List<String> emails) { this.emails = emails; }
    public void addEmail(String email) { this.emails.add(email); }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }
    public void addPhoneNumber(String phone) { this.phoneNumbers.add(phone); }

    public List<String> getLinks() { return links; }
    public void setLinks(List<String> links) { this.links = links; }
    public void addLink(String link) { this.links.add(link); }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public void addImage(String image) { this.images.add(image); }

    public boolean hasContactForm() { return hasContactForm; }
    public void setHasContactForm(boolean hasContactForm) { this.hasContactForm = hasContactForm; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getContentLength() { return contentLength; }

    public long getCrawlTime() { return crawlTime; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public String getDomain() { return domain; }

    // Utility methods
    public String toCSV() {
        return String.format("\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,%d,%d,\"%s\",%b,%d,%s",
            escapeCSV(url),
            escapeCSV(title),
            escapeCSV(description),
            wordCount,
            linkCount,
            imageCount,
            headings.size(),
            emails.size(),
            phoneNumbers.size(),
            escapeCSV(domain),
            hasContactForm,
            depth,
            new java.util.Date(crawlTime).toString()
        );
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
    }

    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append(String.format("  \"url\": \"%s\",\n", escapeJSON(url)));
        json.append(String.format("  \"title\": \"%s\",\n", escapeJSON(title)));
        json.append(String.format("  \"description\": \"%s\",\n", escapeJSON(description)));
        json.append(String.format("  \"domain\": \"%s\",\n", escapeJSON(domain)));
        json.append(String.format("  \"depth\": %d,\n", depth));
        json.append(String.format("  \"wordCount\": %d,\n", wordCount));
        json.append(String.format("  \"linkCount\": %d,\n", linkCount));
        json.append(String.format("  \"imageCount\": %d,\n", imageCount));
        json.append(String.format("  \"headingCount\": %d,\n", headings.size()));
        json.append(String.format("  \"emailCount\": %d,\n", emails.size()));
        json.append(String.format("  \"phoneCount\": %d,\n", phoneNumbers.size()));
        json.append(String.format("  \"hasContactForm\": %b,\n", hasContactForm));
        json.append(String.format("  \"contentLength\": %d,\n", contentLength));
        json.append(String.format("  \"crawlTime\": \"%s\",\n", new java.util.Date(crawlTime)));
        
        // Add arrays
        json.append("  \"headings\": [" + listToJSON(headings) + "],\n");
        json.append("  \"emails\": [" + listToJSON(emails) + "],\n");
        json.append("  \"phoneNumbers\": [" + listToJSON(phoneNumbers) + "],\n");
        json.append("  \"links\": [" + listToJSON(links) + "],\n");
        json.append("  \"images\": [" + listToJSON(images) + "]\n");
        
        json.append("}");
        return json.toString();
    }

    private String escapeJSON(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String listToJSON(List<String> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJSON(list.get(i))).append("\"");
        }
        return sb.toString();
    }
}
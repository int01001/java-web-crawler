package com.webcrawler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class WebCrawlerSwingApp extends JFrame {
    private JTextField urlField;
    private JTextField maxPagesField;
    private JTextField maxDepthField;
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private ExecutorService executorService;
    private WebCrawler crawler;
    private volatile boolean crawling = false;

    public WebCrawlerSwingApp() {
        setTitle("Java Swing Web Crawler");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel northPanel = new JPanel();
        northPanel.add(new JLabel("Seed URL:"));
        urlField = new JTextField("https://example.com", 40);
        northPanel.add(urlField);
        northPanel.add(new JLabel("Max Pages:"));
        maxPagesField = new JTextField("30", 4);
        northPanel.add(maxPagesField);
        northPanel.add(new JLabel("Max Depth:"));
        maxDepthField = new JTextField("2", 2);
        northPanel.add(maxDepthField);

        startButton = new JButton("Start Crawl");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        JPanel southPanel = new JPanel();
        southPanel.add(startButton);
        southPanel.add(stopButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        startButton.addActionListener(this::startCrawlingTask);
        stopButton.addActionListener(e -> crawling = false);
    }

    private void startCrawlingTask(ActionEvent evt) {
        String seedUrl = urlField.getText().trim();
        int maxPages = Integer.parseInt(maxPagesField.getText().trim());
        int maxDepth = Integer.parseInt(maxDepthField.getText().trim());

        logArea.setText("");
        crawling = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> runCrawler(seedUrl, maxPages, maxDepth));
    }

    private void runCrawler(String seedUrl, int maxPages, int maxDepth) {
        try {
            CrawlerConfig config = new CrawlerConfig.Builder()
                .maxPages(maxPages)
                .maxDepth(maxDepth)
                .maxThreads(8)
                .delayBetweenRequests(1000)
                .userAgent("Mozilla/5.0 (compatible; GUIWebCrawler/1.0)")
                .build();

            crawler = new WebCrawler(config);

            // LOGGING: Redirect all output to GUI
            crawler.setLogConsumer(message -> SwingUtilities.invokeLater(() -> {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }));

            crawling = true;
            crawler.startCrawling(seedUrl);

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> logArea.append("Error: " + ex.getMessage() + "\n"));
        } finally {
            crawling = false;
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WebCrawlerSwingApp().setVisible(true));
    }
}

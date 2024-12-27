package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        createShutDownHook();

        CrawlHistoryPanel crawlHistoryPanel = new CrawlHistoryPanel();
        crawlHistoryPanel.startCrawling();

        // Blocks the main thread from ending indefinitely so crawlHistory can keep listening
        Thread.currentThread().join();
    }

    private static void createShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // cleanup logic
            System.out.println("Running cleanup tasks...");
            CrawlHistoryPanel.driver.quit();
            CrawlHistoryPanel.executorService.shutdown();
        }));
    }
}
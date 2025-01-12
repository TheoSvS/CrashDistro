package org.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CrashGameMonitor crashGameMonitor = new CrashGameMonitor();
        createShutDownHook(crashGameMonitor);
        crashGameMonitor.startMonitoring();
        // Blocks the main thread from ending indefinitely so crawlHistory can keep listening
        Thread.currentThread().join();
    }

    private static void createShutDownHook(CrashGameMonitor crashGameMonitor) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // cleanup logic
            System.out.println("Running cleanup tasks...");
            crashGameMonitor.getDriver().quit();
            crashGameMonitor.getExecutorService().shutdown();
        }));
    }
}
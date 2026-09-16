package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.cleaner.DeepCleanManager;
import java.util.Arrays;
import java.util.List;

public class DeepCleanManagerTest {

    public static void run() {
        System.out.println("--------------------------------------------------");
        System.out.println("Running DeepCleanManagerTest...");
        System.out.println("--------------------------------------------------");

        testByteFormatting();
        testJunkOverviewMath();
        testAppJunkInfoModels();
        testCleaningQueueLifecycle();

        System.out.println("✅ All DeepCleanManagerTest assertions PASSED!");
    }

    private static void testByteFormatting() {
        assertEq(DeepCleanManager.formatBytes(0), "0 B", "0 bytes format");
        assertEq(DeepCleanManager.formatBytes(512), "512 B", "512 bytes format");
        assertEq(DeepCleanManager.formatBytes(1024 * 512), "512.0 KB", "512 KB format");
        assertEq(DeepCleanManager.formatBytes(1024 * 1024 * 250), "250.0 MB", "250 MB format");
        assertEq(DeepCleanManager.formatBytes(1024L * 1024L * 1024L * 3L), "3.00 GB", "3 GB format");
        System.out.println("  ✓ testByteFormatting passed");
    }

    private static void testJunkOverviewMath() {
        DeepCleanManager.JunkOverview overview = new DeepCleanManager.JunkOverview();
        overview.totalAppCacheBytes = 1024L * 1024L * 500L; // 500 MB
        overview.thumbnailBytes = 1024L * 1024L * 120L;     // 120 MB
        overview.tempLogBytes = 1024L * 1024L * 30L;        // 30 MB
        overview.apkResidualBytes = 1024L * 1024L * 50L;    // 50 MB
        overview.totalReclaimableBytes = overview.totalAppCacheBytes
            + overview.thumbnailBytes
            + overview.tempLogBytes
            + overview.apkResidualBytes;

        assertEq(overview.totalReclaimableBytes, 1024L * 1024L * 700L, "Total reclaimable must equal 700 MB");
        assertEq(overview.getFormattedTotal(), "700.0 MB", "Formatted total reclaimable");
        assertEq(overview.getFormattedAppCache(), "500.0 MB", "Formatted app cache");
        assertEq(overview.getFormattedThumbnails(), "120.0 MB", "Formatted thumbnails");
        assertEq(overview.getFormattedTempLogs(), "30.0 MB", "Formatted temp logs");
        assertEq(overview.getFormattedApks(), "50.0 MB", "Formatted APKs");
        System.out.println("  ✓ testJunkOverviewMath passed");
    }

    private static void testAppJunkInfoModels() {
        DeepCleanManager.AppJunkInfo info = new DeepCleanManager.AppJunkInfo(
            "com.google.android.youtube",
            "YouTube",
            null,
            1024L * 1024L * 450L,  // 450 MB cache
            1024L * 1024L * 800L,  // 800 MB data
            1024L * 1024L * 120L,  // 120 MB app code
            false
        );

        assertEq(info.totalBytes, 1024L * 1024L * 1370L, "Total storage must equal sum of cache+data+app");
        assertEq(info.getFormattedCache(), "450.0 MB", "Formatted cache");
        assertEq(info.appName, "YouTube", "App name");
        assertEq(info.isCleaned, false, "Initially not cleaned");
        info.isCleaned = true;
        assertEq(info.isCleaned, true, "Marked as cleaned");
        System.out.println("  ✓ testAppJunkInfoModels passed");
    }

    private static void testCleaningQueueLifecycle() {
        assertEq(DeepCleanManager.isAutomatedCleaningRunning(), false, "Initial state not running");
        List<String> pkgs = Arrays.asList("com.android.chrome", "com.google.android.youtube");
        
        DeepCleanManager.startAutomatedClean(null, pkgs, null);
        // With null context it shouldn't crash
        assertEq(DeepCleanManager.isAutomatedCleaningRunning(), false, "Null context does not start queue");

        DeepCleanManager.stopAutomatedClean(null);
        assertEq(DeepCleanManager.isAutomatedCleaningRunning(), false, "Stopped state");
        assertEq(DeepCleanManager.getCurrentCleaningPackage(), null, "No current package when stopped");
        System.out.println("  ✓ testCleaningQueueLifecycle passed");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (actual == null ? expected != null : !actual.equals(expected)) {
            throw new AssertionError("FAILED: " + msg + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}

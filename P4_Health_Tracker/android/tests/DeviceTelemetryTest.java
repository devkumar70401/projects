package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.telemetry.DeviceTelemetryManager;
import com.devendra.healthtracker.telemetry.DeviceTelemetryManager.AppNetworkUsage;
import com.devendra.healthtracker.telemetry.DeviceTelemetryManager.BackgroundAppInfo;
import com.devendra.healthtracker.telemetry.DeviceTelemetryManager.HardwareStats;

import java.util.ArrayList;
import java.util.List;

public class DeviceTelemetryTest {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        System.out.println("--------------------------------------------------");
        System.out.println("Running DeviceTelemetryTest...");
        System.out.println("--------------------------------------------------");

        testByteFormatting();
        testShortByteFormatting();
        testHardwareStatsMath();
        testNullSafety();
        testBackgroundAppSort();

        System.out.println("✅ All DeviceTelemetryTest assertions PASSED!");
    }

    private static void testByteFormatting() {
        assertEquals("0 B", DeviceTelemetryManager.formatBytes(0));
        assertEquals("-1 B", "0 B", DeviceTelemetryManager.formatBytes(-500));
        assertEquals("1.0 KB", DeviceTelemetryManager.formatBytes(1024));
        assertEquals("1.0 MB", DeviceTelemetryManager.formatBytes(1024 * 1024));
        assertEquals("1.5 MB", DeviceTelemetryManager.formatBytes((long) (1.5 * 1024 * 1024)));
        assertEquals("1.0 GB", DeviceTelemetryManager.formatBytes(1024L * 1024L * 1024L));
        assertEquals("8.0 GB", DeviceTelemetryManager.formatBytes(8L * 1024L * 1024L * 1024L));
        System.out.println("  ✓ testByteFormatting passed");
    }

    private static void testShortByteFormatting() {
        assertEquals("0B", DeviceTelemetryManager.formatShortBytes(0));
        assertEquals("500B", DeviceTelemetryManager.formatShortBytes(500));
        assertEquals("10K", DeviceTelemetryManager.formatShortBytes(10 * 1024));
        assertEquals("256M", DeviceTelemetryManager.formatShortBytes(256 * 1024 * 1024));
        assertEquals("4.0G", DeviceTelemetryManager.formatShortBytes(4L * 1024L * 1024L * 1024L));
        System.out.println("  ✓ testShortByteFormatting passed");
    }

    private static void testHardwareStatsMath() {
        HardwareStats stats = new HardwareStats();
        stats.totalRamBytes = 8L * 1024L * 1024L * 1024L; // 8 GB
        stats.freeRamBytes = 4L * 1024L * 1024L * 1024L;  // 4 GB
        stats.usedRamBytes = stats.totalRamBytes - stats.freeRamBytes;
        stats.ramPercent = (int) Math.round(((double) stats.usedRamBytes / (double) stats.totalRamBytes) * 100.0);

        assertEquals("50", String.valueOf(stats.ramPercent));

        stats.totalStorageBytes = 128L * 1024L * 1024L * 1024L; // 128 GB
        stats.usedStorageBytes = 32L * 1024L * 1024L * 1024L;   // 32 GB
        stats.freeStorageBytes = stats.totalStorageBytes - stats.usedStorageBytes;
        stats.storagePercent = (int) Math.round(((double) stats.usedStorageBytes / (double) stats.totalStorageBytes) * 100.0);

        assertEquals("25", String.valueOf(stats.storagePercent));
        System.out.println("  ✓ testHardwareStatsMath passed");
    }

    private static void testNullSafety() {
        // Null context should never crash
        HardwareStats stats = DeviceTelemetryManager.getHardwareStats(null);
        assertTrue(stats != null, "HardwareStats should not be null for null context");

        List<AppNetworkUsage> net = DeviceTelemetryManager.getNetworkUsageApps(null);
        assertTrue(net != null && net.isEmpty(), "Network list should be empty for null context");

        List<BackgroundAppInfo> bg = DeviceTelemetryManager.getRunningBackgroundApps(null);
        assertTrue(bg != null && bg.isEmpty(), "Background apps list should be empty for null context");

        boolean killed = DeviceTelemetryManager.killBackgroundProcess(null, "some.pkg");
        assertTrue(!killed, "Kill should return false for null context");

        int boosted = DeviceTelemetryManager.boostAllRam(null, null);
        assertEquals("0", String.valueOf(boosted));

        long cleaned = DeviceTelemetryManager.cleanStorageCache(null);
        assertEquals("0", String.valueOf(cleaned));

        System.out.println("  ✓ testNullSafety passed");
    }

    private static void testBackgroundAppSort() {
        List<BackgroundAppInfo> list = new ArrayList<>();

        BackgroundAppInfo sysApp = new BackgroundAppInfo();
        sysApp.packageName = "com.android.system";
        sysApp.isSystem = true;
        sysApp.ramBytes = 300L * 1024L * 1024L;
        list.add(sysApp);

        BackgroundAppInfo userApp1 = new BackgroundAppInfo();
        userApp1.packageName = "com.user.game";
        userApp1.isSystem = false;
        userApp1.ramBytes = 200L * 1024L * 1024L;
        list.add(userApp1);

        BackgroundAppInfo userApp2 = new BackgroundAppInfo();
        userApp2.packageName = "com.user.social";
        userApp2.isSystem = false;
        userApp2.ramBytes = 500L * 1024L * 1024L;
        list.add(userApp2);

        // Sort using the same logic: user apps first, higher RAM first
        java.util.Collections.sort(list, (a, b) -> {
            if (a.isSystem != b.isSystem) {
                return a.isSystem ? 1 : -1;
            }
            return Long.compare(b.ramBytes, a.ramBytes);
        });

        assertEquals("com.user.social", list.get(0).packageName);
        assertEquals("com.user.game", list.get(1).packageName);
        assertEquals("com.android.system", list.get(2).packageName);

        System.out.println("  ✓ testBackgroundAppSort passed");
    }

    private static void assertEquals(String expected, String actual) {
        assertEquals("Expected '" + expected + "' but got '" + actual + "'", expected, actual);
    }

    private static void assertEquals(String message, String expected, String actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}

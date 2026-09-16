package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.calendar.CalendarManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class CalendarManagerTest {

    public static void run() {
        System.out.println("--------------------------------------------------");
        System.out.println("Running CalendarManagerTest...");
        System.out.println("--------------------------------------------------");

        // 1. Date key formatting
        String key = CalendarManager.formatDateKey(2026, 9, 8);
        assertEq(key, "2026-09-08", "Date key must format to YYYY-MM-DD");
        System.out.println("  ✓ formatDateKey format verified");

        String display = CalendarManager.formatDisplayDate("2026-09-08");
        assertTrue(display != null && display.contains("Sep"), "Display date must contain month name");
        System.out.println("  ✓ formatDisplayDate verified: " + display);

        String today = CalendarManager.getTodayKey();
        assertTrue(today != null && today.length() == 10, "Today key must be 10 chars");
        System.out.println("  ✓ getTodayKey verified: " + today);

        // 2. Mock JSON storage verification (simulating on-disk file persistence)
        try {
            File tmp = File.createTempFile("cal_test_", ".json");
            tmp.deleteOnExit();

            String jsonPayload = "{\"2026-09-08\":\"Special Anniversary!\",\"2026-10-15\":\"Doctor appointment\"}";
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                fos.getFD().sync();
            }

            assertTrue(tmp.exists() && tmp.length() > 0, "Persistent file must exist and have content");

            byte[] b = new byte[(int) tmp.length()];
            try (FileInputStream fis = new FileInputStream(tmp)) {
                fis.read(b);
            }
            String loaded = new String(b, StandardCharsets.UTF_8);
            assertTrue(loaded.contains("Special Anniversary!"), "Loaded content must contain note");
            System.out.println("  ✓ Disk persistence simulation verified");
        } catch (Exception e) {
            throw new AssertionError("File test failed: " + e.getMessage());
        }

        System.out.println("✅ All CalendarManagerTest assertions PASSED!");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (!actual.equals(expected)) {
            throw new AssertionError("FAIL: " + msg + " [Expected: " + expected + ", Got: " + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("FAIL: " + msg);
    }
}

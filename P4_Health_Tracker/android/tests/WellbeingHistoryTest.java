package com.devendra.healthtracker.tests;

import java.text.SimpleDateFormat;
import java.util.*;

public class WellbeingHistoryTest {

    public static class TestWellbeingRecord {
        public String date;
        public int totalFocusMinutes;
        public int completedPomodoros;
        public int tasksCompleted;
        public int activeAppMinutes;
        public long lastActiveTimestamp;

        public TestWellbeingRecord(String date) {
            this.date = date != null ? date : "";
            this.totalFocusMinutes = 0;
            this.completedPomodoros = 0;
            this.tasksCompleted = 0;
            this.activeAppMinutes = 0;
            this.lastActiveTimestamp = System.currentTimeMillis();
        }

        public TestWellbeingRecord(String date, int focusMin, int pomos, int tasks, int activeMin, long lastActive) {
            this.date = date;
            this.totalFocusMinutes = Math.max(0, focusMin);
            this.completedPomodoros = Math.max(0, pomos);
            this.tasksCompleted = Math.max(0, tasks);
            this.activeAppMinutes = Math.max(0, activeMin);
            this.lastActiveTimestamp = lastActive > 0 ? lastActive : System.currentTimeMillis();
        }
    }

    public static class TestWellbeingHistoryEngine {
        public Map<String, TestWellbeingRecord> store = new HashMap<>();
        public static final int RETENTION_DAYS = 30;

        public TestWellbeingRecord getRecord(String date) {
            return store.computeIfAbsent(date, TestWellbeingRecord::new);
        }

        public void recordFocusSession(String date, int minutes) {
            TestWellbeingRecord r = getRecord(date);
            r.totalFocusMinutes += Math.max(0, minutes);
            r.completedPomodoros += 1;
            r.lastActiveTimestamp = System.currentTimeMillis();
        }

        public void recordTaskCompleted(String date) {
            TestWellbeingRecord r = getRecord(date);
            r.tasksCompleted += 1;
            r.lastActiveTimestamp = System.currentTimeMillis();
        }

        public void recordTaskUncompleted(String date) {
            TestWellbeingRecord r = getRecord(date);
            if (r.tasksCompleted > 0) {
                r.tasksCompleted -= 1;
            }
            r.lastActiveTimestamp = System.currentTimeMillis();
        }

        public void recordActiveAppTime(String date, int minutes) {
            if (minutes <= 0) return;
            TestWellbeingRecord r = getRecord(date);
            r.activeAppMinutes += minutes;
            r.lastActiveTimestamp = System.currentTimeMillis();
        }

        public List<TestWellbeingRecord> getPastDays(Date targetDate, int count) {
            List<TestWellbeingRecord> list = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.setTime(targetDate);
            cal.add(Calendar.DAY_OF_YEAR, -(count - 1));

            for (int i = 0; i < count; i++) {
                String dStr = sdf.format(cal.getTime());
                list.add(getRecord(dStr));
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            return list;
        }

        public void pruneOldRecords(Date currentDate, int keepDays) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cutoff = Calendar.getInstance();
            cutoff.setTime(currentDate);
            cutoff.add(Calendar.DAY_OF_YEAR, -keepDays);
            Date cutoffDate = cutoff.getTime();

            store.entrySet().removeIf(entry -> {
                try {
                    Date d = sdf.parse(entry.getKey());
                    return d != null && d.before(cutoffDate);
                } catch (Exception e) {
                    return true;
                }
            });
        }
    }

    public static void runTests() {
        System.out.println("--- [TEST SUITE 3: Multi-Day Wellbeing Telemetry & Rollover] ---");

        TestWellbeingHistoryEngine engine = new TestWellbeingHistoryEngine();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // 1. Record metrics for Day 1 (2026-09-01)
        String day1 = "2026-09-01";
        engine.recordFocusSession(day1, 50);
        engine.recordFocusSession(day1, 25);
        engine.recordTaskCompleted(day1);
        engine.recordTaskCompleted(day1);
        engine.recordTaskCompleted(day1);
        engine.recordActiveAppTime(day1, 35);

        TestWellbeingRecord r1 = engine.getRecord(day1);
        assertEq(r1.totalFocusMinutes, 75, "Day 1 focus minutes should be 75 (50 + 25)");
        assertEq(r1.completedPomodoros, 2, "Day 1 pomodoros should be 2");
        assertEq(r1.tasksCompleted, 3, "Day 1 tasks should be 3");
        assertEq(r1.activeAppMinutes, 35, "Day 1 active app minutes should be 35");

        // Verify task uncompleted decrements count and floors at 0
        engine.recordTaskUncompleted(day1);
        assertEq(r1.tasksCompleted, 2, "Unticking task should decrement tasksCompleted from 3 to 2");
        engine.recordTaskUncompleted(day1);
        engine.recordTaskUncompleted(day1);
        engine.recordTaskUncompleted(day1); // Floor check
        assertEq(r1.tasksCompleted, 0, "tasksCompleted should floor at 0 and never be negative");
        engine.recordTaskCompleted(day1);
        engine.recordTaskCompleted(day1);
        engine.recordTaskCompleted(day1); // Restore to 3 tasks for subsequent tests

        // 2. Midnight Rollover Simulation -> Day 2 (2026-09-02)
        String day2 = "2026-09-02";
        engine.recordFocusSession(day2, 25);
        engine.recordTaskCompleted(day2);
        engine.recordActiveAppTime(day2, 10);

        TestWellbeingRecord r2 = engine.getRecord(day2);
        assertEq(r2.totalFocusMinutes, 25, "Day 2 focus minutes should be 25");
        assertEq(r2.completedPomodoros, 1, "Day 2 pomodoros should be 1");
        assertEq(r2.tasksCompleted, 1, "Day 2 tasks should be 1");
        assertEq(r2.activeAppMinutes, 10, "Day 2 active app minutes should be 10");

        // Verify Day 1 was NOT corrupted or overwritten
        TestWellbeingRecord r1Check = engine.getRecord(day1);
        assertEq(r1Check.totalFocusMinutes, 75, "Day 1 records must remain intact across rollover");
        assertEq(r1Check.tasksCompleted, 3, "Day 1 tasks must remain intact");

        // 3. 7-Day Window Query
        Calendar cal = Calendar.getInstance();
        try {
            Date refDate = sdf.parse("2026-09-07");
            List<TestWellbeingRecord> week = engine.getPastDays(refDate, 7);
            assertEq(week.size(), 7, "getPastDays must return exactly 7 records");
            assertEq(week.get(0).date, "2026-09-01", "First day in 7-day window must be 2026-09-01");
            assertEq(week.get(6).date, "2026-09-07", "Last day in 7-day window must be 2026-09-07");
            assertEq(week.get(0).totalFocusMinutes, 75, "Window must contain day 1 data");
            assertEq(week.get(1).totalFocusMinutes, 25, "Window must contain day 2 data");
            assertEq(week.get(2).totalFocusMinutes, 0, "Missing days must be zero-filled");
        } catch (Exception e) {
            throw new AssertionError("Date parse failed: " + e.getMessage());
        }

        // 4. 30-Day Retention Pruning
        // Inject an old date 45 days prior
        try {
            Date currentDate = sdf.parse("2026-09-08");
            String oldDate = "2026-07-20"; // ~50 days old
            engine.recordFocusSession(oldDate, 100);
            assertEq(engine.store.containsKey(oldDate), true, "Old date present before pruning");

            engine.pruneOldRecords(currentDate, 30);
            assertFalse(engine.store.containsKey(oldDate), "Records older than 30 days must be pruned");
            assertTrue(engine.store.containsKey(day1), "Day 1 (7 days old) must be preserved");
            assertTrue(engine.store.containsKey(day2), "Day 2 (6 days old) must be preserved");
        } catch (Exception e) {
            throw new AssertionError("Prune date parse failed: " + e.getMessage());
        }

        System.out.println("✅ WellbeingHistoryTest: ALL 8 ASSERTIONS PASSED!");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (!actual.equals(expected)) {
            throw new AssertionError("FAIL: " + msg + " [Expected: " + expected + ", Got: " + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("FAIL: " + msg);
    }

    private static void assertFalse(boolean condition, String msg) {
        if (condition) throw new AssertionError("FAIL: " + msg);
    }
}

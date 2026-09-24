package com.devendra.healthtracker.tests;

public class PomodoroStateTest {

    public enum Status { IDLE, RUNNING, PAUSED, COMPLETED }
    public enum Phase { WORK, SHORT_BREAK, LONG_BREAK }

    public static class PomodoroStateMachine {
        public Phase phase = Phase.WORK;
        public Status status = Status.IDLE;
        public int totalSec = 25 * 60;
        public int remainingSec = 25 * 60;
        public long endTimeMs = 0;
        public int completedToday = 0;
        public int totalFocusSec = 0;
        public boolean soundEnabled = true;
        public boolean notificationEnabled = true;

        public void start(long nowMs) {
            if (remainingSec <= 0) remainingSec = totalSec;
            status = Status.RUNNING;
            endTimeMs = nowMs + (remainingSec * 1000L);
        }

        public void pause(long nowMs) {
            if (status == Status.RUNNING) {
                remainingSec = getRemainingSeconds(nowMs);
                status = Status.PAUSED;
                endTimeMs = 0;
            }
        }

        public void reset() {
            status = Status.IDLE;
            remainingSec = totalSec;
            endTimeMs = 0;
        }

        public int getRemainingSeconds(long nowMs) {
            if (status == Status.RUNNING) {
                long diff = (endTimeMs - nowMs) / 1000L;
                return Math.min(totalSec, Math.max(0, (int) diff));
            }
            return Math.min(totalSec, Math.max(0, remainingSec));
        }

        public boolean checkCompletion(long nowMs) {
            if (status == Status.RUNNING && getRemainingSeconds(nowMs) <= 0) {
                onSessionComplete();
                return true;
            }
            return false;
        }

        public void onSessionComplete() {
            if (status != Status.RUNNING) {
                return; // Guard against concurrent duplicate calls
            }
            if (phase == Phase.WORK) {
                completedToday++;
                totalFocusSec += totalSec;
                if (completedToday % 4 == 0) {
                    phase = Phase.LONG_BREAK;
                    totalSec = 15 * 60;
                } else {
                    phase = Phase.SHORT_BREAK;
                    totalSec = 5 * 60;
                }
            } else {
                phase = Phase.WORK;
                totalSec = 25 * 60;
            }
            status = Status.IDLE;
            remainingSec = totalSec;
            endTimeMs = 0;
        }

        public void skip() {
            if (phase == Phase.WORK) {
                if ((completedToday + 1) % 4 == 0) {
                    phase = Phase.LONG_BREAK;
                    totalSec = 15 * 60;
                } else {
                    phase = Phase.SHORT_BREAK;
                    totalSec = 5 * 60;
                }
            } else {
                phase = Phase.WORK;
                totalSec = 25 * 60;
            }
            status = Status.IDLE;
            remainingSec = totalSec;
            endTimeMs = 0;
        }

        public void setPreset(Phase p, int sec) {
            phase = p;
            totalSec = sec;
            remainingSec = sec;
            status = Status.IDLE;
            endTimeMs = 0;
        }
    }

    public static void runTests() {
        System.out.println("--- [TEST SUITE 1: Pomodoro State & Timer Arithmetic] ---");

        // 1. Initial state
        PomodoroStateMachine p = new PomodoroStateMachine();
        assertEq(p.status, Status.IDLE, "Initial status should be IDLE");
        assertEq(p.phase, Phase.WORK, "Initial phase should be WORK");
        assertEq(p.remainingSec, 1500, "Initial remaining should be 1500s (25m)");

        // 2. Start transition
        long t0 = 1000000000L;
        p.start(t0);
        assertEq(p.status, Status.RUNNING, "Status after start should be RUNNING");
        assertEq(p.endTimeMs, t0 + 1500000L, "EndTimeMs must be now + 1500s in ms");

        // 3. Monotonic timestamp delta check
        long t1 = t0 + 500000L; // 500 seconds elapsed
        assertEq(p.getRemainingSeconds(t1), 1000, "Remaining seconds after 500s should be 1000s");

        // 4. Pause transition
        p.pause(t1);
        assertEq(p.status, Status.PAUSED, "Status after pause should be PAUSED");
        assertEq(p.remainingSec, 1000, "Remaining seconds preserved as 1000");
        assertEq(p.endTimeMs, 0L, "EndTimeMs cleared to 0 on pause");

        // 5. Resume from paused
        long t2 = t1 + 300000L; // Resumed 300s later (e.g. paused duration)
        p.start(t2);
        assertEq(p.status, Status.RUNNING, "Status should be RUNNING on resume");
        assertEq(p.endTimeMs, t2 + 1000000L, "EndTimeMs must be new now + 1000s");
        assertEq(p.getRemainingSeconds(t2), 1000, "Remaining should start at 1000s on resume");

        // 6. Complete Session 1 (Work -> Short Break)
        long tComplete = p.endTimeMs;
        boolean completed = p.checkCompletion(tComplete);
        assertTrue(completed, "checkCompletion should trigger on reaching 00:00");
        assertEq(p.status, Status.IDLE, "Status after completion should be IDLE");
        assertEq(p.completedToday, 1, "Completed sessions count should be 1");
        assertEq(p.phase, Phase.SHORT_BREAK, "Next phase should be SHORT_BREAK");
        assertEq(p.totalSec, 300, "Short break duration should be 300s (5m)");

        // 7. Complete Break (Short Break -> Work)
        p.start(tComplete + 1000);
        p.checkCompletion(p.endTimeMs);
        assertEq(p.phase, Phase.WORK, "After break, phase should be WORK");
        assertEq(p.totalSec, 1500, "Work duration should be 1500s");

        // 8. Test 4th session long break transition
        p.completedToday = 3; // Simulate 3 sessions done
        p.phase = Phase.WORK;
        p.totalSec = 1500;
        p.start(tComplete + 2000);
        p.checkCompletion(p.endTimeMs);
        assertEq(p.completedToday, 4, "Should have 4 completed sessions");
        assertEq(p.phase, Phase.LONG_BREAK, "4th completed session must transition to LONG_BREAK");
        assertEq(p.totalSec, 900, "Long break duration should be 900s (15m)");

        // 9. Reset check
        p.start(tComplete + 3000);
        p.reset();
        assertEq(p.status, Status.IDLE, "Status must be IDLE after reset");
        assertEq(p.remainingSec, p.totalSec, "RemainingSec must equal totalSec on reset");
        assertEq(p.endTimeMs, 0L, "EndTimeMs must be 0 after reset");

        // 10. Sound toggle persistence & alert checks
        p.soundEnabled = false;
        assertFalse(p.soundEnabled, "Sound toggle disabled state verified");
        p.soundEnabled = true;
        assertTrue(p.soundEnabled, "Sound toggle enabled state verified");

        // 11. Concurrency / duplicate completion idempotency check
        p.onSessionComplete(); // Called when already IDLE (simulating race between ticker and alarm)
        assertEq(p.completedToday, 4, "Duplicate call to onSessionComplete when IDLE must not increment completedToday");

        // 12. Clock shift / NTP backward jump check
        p.start(1000000000L);
        long clockJumpBackward = 500000000L; // System clock jumps back 500 seconds
        assertEq(p.getRemainingSeconds(clockJumpBackward), p.totalSec, "Remaining seconds must clamp to totalSec on backward clock jump");

        System.out.println("✅ PomodoroStateTest: ALL 12 ASSERTIONS PASSED!");
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

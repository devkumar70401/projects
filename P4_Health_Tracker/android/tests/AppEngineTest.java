package com.devendra.healthtracker.tests;

import java.util.HashMap;
import java.util.Map;

public class AppEngineTest {

    public static class MockAppEngineManager {
        private final Map<String, Boolean> prefs = new HashMap<>();
        private static final String KEY_APP_ENABLED = "app_engine_enabled";
        public int activeScheduledAlarms = 0;
        public boolean backgroundTrackingActive = false;

        public boolean isAppEnabled() {
            return prefs.getOrDefault(KEY_APP_ENABLED, true);
        }

        public void setAppEnabled(boolean enabled) {
            prefs.put(KEY_APP_ENABLED, enabled);
            if (enabled) {
                scheduleAllAlarms();
                backgroundTrackingActive = true;
            } else {
                cancelAllAlarms();
                backgroundTrackingActive = false;
            }
        }

        public void scheduleAllAlarms() {
            if (!isAppEnabled()) return;
            activeScheduledAlarms = 13;
        }

        public void cancelAllAlarms() {
            activeScheduledAlarms = 0;
        }
    }

    public static void run() {
        MockAppEngineManager engine = new MockAppEngineManager();

        // 1. Initial State: App is enabled by default
        assertEq(engine.isAppEnabled(), true, "App engine must be enabled by default");

        // 2. Schedule alarms in enabled state
        engine.scheduleAllAlarms();
        assertEq(engine.activeScheduledAlarms, 13, "13 cadence alarms must be scheduled when enabled");

        // 3. Toggle App Engine OFF
        engine.setAppEnabled(false);
        assertEq(engine.isAppEnabled(), false, "App engine must be disabled after toggle OFF");
        assertEq(engine.activeScheduledAlarms, 0, "All alarms must be cancelled when engine toggled OFF");
        assertEq(engine.backgroundTrackingActive, false, "Background tracking must be paused when engine toggled OFF");

        // 4. Guard check: calling scheduleAllAlarms when OFF should not schedule
        engine.scheduleAllAlarms();
        assertEq(engine.activeScheduledAlarms, 0, "Alarms must NOT be scheduled if engine is disabled");

        // 5. Toggle App Engine ON
        engine.setAppEnabled(true);
        assertEq(engine.isAppEnabled(), true, "App engine must be enabled after toggle ON");
        assertEq(engine.activeScheduledAlarms, 13, "All 13 alarms must be re-scheduled when engine toggled ON");
        assertEq(engine.backgroundTrackingActive, true, "Background tracking must be active when engine toggled ON");

        System.out.println("✅ AppEngineTest: ALL 8 ASSERTIONS PASSED!");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (actual == null ? expected != null : !actual.equals(expected)) {
            throw new AssertionError("FAILED: " + msg + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}

package com.devendra.healthtracker.tests;

public class RunAllTests {
    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("ROSE HEALTH TRACKER: AUTOMATED VERIFICATION & QUALITY ASSURANCE SUITE");
        System.out.println("======================================================================\n");

        long start = System.currentTimeMillis();

        try {
            PomodoroStateTest.runTests();
            System.out.println();
            ChecklistManagerTest.runTests();
            System.out.println();
            WellbeingHistoryTest.runTests();
            System.out.println();
            AppEngineTest.run();
            System.out.println();
            ScheduleManagerTest.run();
            System.out.println();
            DeviceTelemetryTest.run();
            System.out.println();
            DeepCleanManagerTest.run();
            System.out.println();
            MediaDownloaderEngineTest.run();
            System.out.println();
            CalculatorEngineTest.runAll();
            System.out.println();
            ThemeManagerTest.run();
            System.out.println();
            FeatureOrderManagerTest.run();
            System.out.println();
            CalendarManagerTest.run();
            System.out.println();

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("======================================================================");
            System.out.println("🎉 ALL TEST SUITES PASSED CLEANLY (Completed in " + elapsed + " ms)!");
            System.out.println("======================================================================");
        } catch (Throwable t) {
            System.err.println("\n❌ TEST SUITE FAILED:");
            t.printStackTrace();
            System.exit(1);
        }
    }
}

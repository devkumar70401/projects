package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.routine.ScheduleManager;

public class ScheduleManagerTest {

    public static void run() {
        // 1. Baseline: 8:00 AM Wake, 12:00 AM Sleep (16h waking, 8h sleep)
        ScheduleManager.recalculatePhases(8, 0, 0, 0);
        assertEq(ScheduleManager.PHASES.size(), 14, "Must have exactly 14 phases");

        ScheduleManager.Phase p1 = ScheduleManager.PHASES.get(0);
        assertEq(p1.startHour, 8, "Phase 1 start hour must be 8");
        assertEq(p1.startMinute, 0, "Phase 1 start min must be 0");
        assertEq(p1.endHour, 8, "Phase 1 end hour must be 8");
        assertEq(p1.endMinute, 30, "Phase 1 end min must be 30");

        // Verify English Practice phase (index 9, phase-10)
        ScheduleManager.Phase pEnglish = ScheduleManager.PHASES.get(9);
        assertEq(pEnglish.id, "phase-10", "Phase 10 ID must be phase-10");
        assertEq(pEnglish.startHour, 19, "English Practice start hour must be 19 (7:30 PM)");
        assertEq(pEnglish.startMinute, 30, "English Practice start min must be 30");
        assertEq(pEnglish.endHour, 20, "English Practice end hour must be 20 (8:30 PM)");
        assertEq(pEnglish.endMinute, 30, "English Practice end min must be 30");
        assertEq(pEnglish.title.contains("English"), true, "Phase 10 title must contain 'English'");

        ScheduleManager.Phase p13 = ScheduleManager.PHASES.get(12);
        assertEq(p13.startHour, 23, "Phase 13 start hour must be 23");
        assertEq(p13.startMinute, 30, "Phase 13 start min must be 30");
        assertEq(p13.endHour, 0, "Phase 13 end hour must be 0 (12:00 AM)");
        assertEq(p13.endMinute, 0, "Phase 13 end min must be 0");

        ScheduleManager.Phase p14 = ScheduleManager.PHASES.get(13);
        assertEq(p14.startHour, 0, "Phase 14 start hour must be 0");
        assertEq(p14.endHour, 8, "Phase 14 end hour must be 8");
        assertEq(p14.title.contains("8 Hours") || p14.title.contains("8.0 Hours"), true, "Phase 14 must report 8 Hours of sleep");

        // 2. Custom Timings with identical distance: 07:00 AM Wake, 11:00 PM Sleep (16h waking)
        ScheduleManager.recalculatePhases(7, 0, 23, 0);
        assertEq(ScheduleManager.PHASES.size(), 14, "Shifted schedule must have 14 phases");

        ScheduleManager.Phase shiftedP1 = ScheduleManager.PHASES.get(0);
        assertEq(shiftedP1.startHour, 7, "Shifted Phase 1 start hour must be 7");
        assertEq(shiftedP1.startMinute, 0, "Shifted Phase 1 start min must be 0");
        assertEq(shiftedP1.endHour, 7, "Shifted Phase 1 end hour must be 7");
        assertEq(shiftedP1.endMinute, 30, "Shifted Phase 1 end min must be 30");

        ScheduleManager.Phase shiftedPEnglish = ScheduleManager.PHASES.get(9);
        assertEq(shiftedPEnglish.startHour, 18, "Shifted English Practice start hour must be 18 (6:30 PM)");
        assertEq(shiftedPEnglish.startMinute, 30, "Shifted English Practice start min must be 30");
        assertEq(shiftedPEnglish.endHour, 19, "Shifted English Practice end hour must be 19 (7:30 PM)");
        assertEq(shiftedPEnglish.endMinute, 30, "Shifted English Practice end min must be 30");

        ScheduleManager.Phase shiftedP13 = ScheduleManager.PHASES.get(12);
        assertEq(shiftedP13.startHour, 22, "Shifted Phase 13 start hour must be 22 (10:30 PM)");
        assertEq(shiftedP13.startMinute, 30, "Shifted Phase 13 start min must be 30");
        assertEq(shiftedP13.endHour, 23, "Shifted Phase 13 end hour must be 23 (11:00 PM)");
        assertEq(shiftedP13.endMinute, 0, "Shifted Phase 13 end min must be 0");

        ScheduleManager.Phase shiftedP14 = ScheduleManager.PHASES.get(13);
        assertEq(shiftedP14.startHour, 23, "Shifted Phase 14 start hour must be 23 (11:00 PM)");
        assertEq(shiftedP14.endHour, 7, "Shifted Phase 14 end hour must be 7 (07:00 AM)");
        assertEq(shiftedP14.title.contains("8 Hours") || shiftedP14.title.contains("8.0 Hours"), true, "Shifted Phase 14 must report 8 Hours of sleep");

        // 3. 24-Hour Continuous Cycle Integrity Verification
        int totalMinutes = 0;
        for (ScheduleManager.Phase p : ScheduleManager.PHASES) {
            int start = p.startHour * 60 + p.startMinute;
            int end = p.endHour * 60 + p.endMinute;
            int dur = (end - start + 1440) % 1440;
            if (dur == 0) dur = 1440;
            totalMinutes += dur;
        }
        assertEq(totalMinutes, 1440, "Sum of all 14 phases must exactly equal 1440 minutes (24 hours)");

        // 4. Extreme short waking window (2h awake): Wake 08:00, Sleep 10:00 -> fallback to 16h circadian guard
        ScheduleManager.recalculatePhases(8, 0, 10, 0);
        assertEq(ScheduleManager.PHASES.size(), 14, "Extreme schedule must still produce 14 phases");
        int sumExtreme = 0;
        for (int i = 0; i < ScheduleManager.PHASES.size(); i++) {
            ScheduleManager.Phase curr = ScheduleManager.PHASES.get(i);
            ScheduleManager.Phase next = ScheduleManager.PHASES.get((i + 1) % 14);
            assertEq(curr.endHour, next.startHour, "Phase " + i + " endHour must match Phase " + ((i + 1) % 14) + " startHour");
            assertEq(curr.endMinute, next.startMinute, "Phase " + i + " endMinute must match Phase " + ((i + 1) % 14) + " startMinute");
            int s = curr.startHour * 60 + curr.startMinute;
            int e = curr.endHour * 60 + curr.endMinute;
            int d = (e - s + 1440) % 1440;
            if (d == 0) d = 1440;
            sumExtreme += d;
        }
        assertEq(sumExtreme, 1440, "Extreme 2h schedule must fallback and sum to exactly 1440 minutes");

        // 5. Extreme long waking window (23.5h awake): Wake 08:00, Sleep 07:30 -> fallback to 16h circadian guard
        ScheduleManager.recalculatePhases(8, 0, 7, 30);
        int sumExtremeLong = 0;
        for (ScheduleManager.Phase p : ScheduleManager.PHASES) {
            int s = p.startHour * 60 + p.startMinute;
            int e = p.endHour * 60 + p.endMinute;
            int d = (e - s + 1440) % 1440;
            if (d == 0) d = 1440;
            sumExtremeLong += d;
        }
        assertEq(sumExtremeLong, 1440, "Extreme 23.5h schedule must fallback and sum to exactly 1440 minutes");

        // 6. Valid custom window: 14h awake (06:00 to 20:00)
        ScheduleManager.recalculatePhases(6, 0, 20, 0);
        int sumValidCustom = 0;
        for (int i = 0; i < ScheduleManager.PHASES.size(); i++) {
            ScheduleManager.Phase curr = ScheduleManager.PHASES.get(i);
            ScheduleManager.Phase next = ScheduleManager.PHASES.get((i + 1) % 14);
            assertEq(curr.endHour, next.startHour, "Phase " + i + " endHour must match Phase " + ((i + 1) % 14) + " startHour in 14h day");
            assertEq(curr.endMinute, next.startMinute, "Phase " + i + " endMinute must match Phase " + ((i + 1) % 14) + " startMinute in 14h day");
            int s = curr.startHour * 60 + curr.startMinute;
            int e = curr.endHour * 60 + curr.endMinute;
            int d = (e - s + 1440) % 1440;
            if (d == 0) d = 1440;
            sumValidCustom += d;
        }
        assertEq(sumValidCustom, 1440, "Valid 14h custom schedule must sum to exactly 1440 minutes");

        // Restore default
        ScheduleManager.recalculatePhases(8, 0, 0, 0);

        // 7. Granular Routine Blocks & Ingredient Isolation Rigor
        java.util.List<com.devendra.healthtracker.routine.RoutineBlock> allBlocks = ScheduleManager.getAllGranularBlocks();
        assertEq(allBlocks.size() >= 30, true, "Must have at least 30 granular blocks across 24-hour cycle");

        // Verify dedicated Wakeup block
        com.devendra.healthtracker.routine.RoutineBlock wakeupBlock = null;
        for (com.devendra.healthtracker.routine.RoutineBlock b : allBlocks) {
            if ("block-01-wakeup".equals(b.id)) {
                wakeupBlock = b;
                break;
            }
        }
        assertEq(wakeupBlock != null, true, "Must contain dedicated Wake Up block (block-01-wakeup)");
        assertEq("WAKE".equals(wakeupBlock.category), true, "Wakeup block category must be WAKE");

        // Verify Brush, Face Wash and Bath blocks
        com.devendra.healthtracker.routine.RoutineBlock brushBlock = null;
        com.devendra.healthtracker.routine.RoutineBlock facewashBlock = null;
        com.devendra.healthtracker.routine.RoutineBlock bathBlock = null;
        for (com.devendra.healthtracker.routine.RoutineBlock b : allBlocks) {
            if ("block-01-brush".equals(b.id)) brushBlock = b;
            if ("block-01-facewash".equals(b.id)) facewashBlock = b;
            if ("block-01-bath".equals(b.id)) bathBlock = b;
        }
        assertEq(brushBlock != null, true, "Must contain dedicated Brush block (block-01-brush)");
        assertEq(facewashBlock != null, true, "Must contain dedicated Face Wash block (block-01-facewash)");
        assertEq(bathBlock != null, true, "Must contain dedicated Bath block (block-01-bath)");
        assertEq("ROUTINE".equals(brushBlock.category), true, "Brush block category must be ROUTINE");
        assertEq("ROUTINE".equals(facewashBlock.category), true, "Face Wash block category must be ROUTINE");
        assertEq("ROUTINE".equals(bathBlock.category), true, "Bath block category must be ROUTINE");

        // Verify dedicated Sleep block
        com.devendra.healthtracker.routine.RoutineBlock sleepBlock = null;
        for (com.devendra.healthtracker.routine.RoutineBlock b : allBlocks) {
            if ("block-14-deep-sleep".equals(b.id)) {
                sleepBlock = b;
                break;
            }
        }
        assertEq(sleepBlock != null, true, "Must contain dedicated Sleep block (block-14-deep-sleep)");
        assertEq("SLEEP".equals(sleepBlock.category), true, "Sleep block category must be SLEEP");

        // Verify Phase 1 blocks count
        ScheduleManager.Phase phase1 = ScheduleManager.PHASES.get(0);
        assertEq(phase1.granularBlocks.size(), 7, "Phase 1 must contain exactly 7 granular blocks (Wakeup, Brush, Face Wash, Bath, Water, Salt, Lemon)");

        // Verify Fuel Phase 1 ingredients separated
        ScheduleManager.Phase p3 = ScheduleManager.PHASES.get(2);
        assertEq(p3.id, "phase-03", "Phase 3 must be phase-03");
        assertEq(p3.granularBlocks.size(), 9, "Fuel Phase 1 must contain exactly 9 granular ingredient blocks");
        assertEq(p3.granularBlocks.get(0).id, "block-03-chia", "First ingredient must be Chia Tonic");
        assertEq(p3.granularBlocks.get(1).id, "block-03-almonds", "Second ingredient must be Almonds");
        assertEq(p3.granularBlocks.get(2).id, "block-03-walnuts", "Third ingredient must be Walnuts");
        assertEq(p3.granularBlocks.get(3).id, "block-03-banana", "Fourth ingredient must be Banana");
        assertEq(p3.granularBlocks.get(4).id, "block-03-pb-roti", "Fifth ingredient must be PB Roti");
        assertEq(p3.granularBlocks.get(5).id, "block-03-peanuts", "Sixth ingredient must be Peanuts");
        assertEq(p3.granularBlocks.get(6).id, "block-03-chana", "Seventh ingredient must be Chana");
        assertEq(p3.granularBlocks.get(7).id, "block-03-moringa", "Eighth ingredient must be Moringa Powder");
        assertEq(p3.granularBlocks.get(8).id, "block-03-d3", "Ninth ingredient must be Dr. Excel D3 Weekly Protocol");

        // Verify Fuel Phase 2 ingredients separated
        ScheduleManager.Phase p5 = ScheduleManager.PHASES.get(4);
        assertEq(p5.id, "phase-05", "Phase 5 must be phase-05");
        assertEq(p5.granularBlocks.size(), 4, "Fuel Phase 2 must contain exactly 4 granular ingredient blocks");
        assertEq(p5.granularBlocks.get(0).id, "block-05-soya", "Must have Soya Chunks block");
        assertEq(p5.granularBlocks.get(1).id, "block-05-veggies", "Must have Veggies block");
        assertEq(p5.granularBlocks.get(2).id, "block-05-roti", "Must have 1 Roti cap block");
        assertEq(p5.granularBlocks.get(3).id, "block-05-lemon", "Must have Lemon Vitamin C catalyst block");

        // Verify Fuel Phase 3 ingredients separated
        ScheduleManager.Phase p8 = ScheduleManager.PHASES.get(7);
        assertEq(p8.id, "phase-08", "Phase 8 must be phase-08");
        assertEq(p8.granularBlocks.size(), 4, "Fuel Phase 3 must contain exactly 4 clean ingredient blocks (Eggs, Pumpkin, Lemon, Creatine)");
        assertEq(p8.granularBlocks.get(0).id, "block-08-eggs-paneer", "Must have 4 Eggs/Paneer block");
        assertEq(p8.granularBlocks.get(1).id, "block-08-pumpkin", "Must have Pumpkin Seeds block");
        assertEq(p8.granularBlocks.get(2).id, "block-08-lemon", "Must have Lemon block");
        assertEq(p8.granularBlocks.get(3).id, "block-08-creatine", "Must have Creatine block");

        // Verify Fuel Phase 4 ingredients separated
        ScheduleManager.Phase p11 = ScheduleManager.PHASES.get(10);
        assertEq(p11.id, "phase-11", "Phase 11 must be phase-11");
        assertEq(p11.granularBlocks.size(), 2, "Fuel Phase 4 must contain exactly 2 granular ingredient blocks (Khichdi + Curd)");

        // Verify Phase 13 wind-down blocks
        ScheduleManager.Phase phase13 = ScheduleManager.PHASES.get(12);
        assertEq(phase13.id, "phase-13", "Phase 13 must be phase-13");
        assertEq(phase13.granularBlocks.size(), 3, "Phase 13 must contain exactly 3 granular blocks (Night Water, Dahi Prep, Sleep Prep)");
        assertEq(phase13.granularBlocks.get(0).id, "block-13-night-water", "First block must be Night Water");
        assertEq(phase13.granularBlocks.get(1).id, "block-13-dahi-prep", "Second block must be Dahi Prep");
        assertEq(phase13.granularBlocks.get(2).id, "block-13-sleep-prep", "Third block must be Sleep Prep");

        // Verify RoutineBlock field integrity
        assertEq(wakeupBlock.id, "block-01-wakeup", "Wakeup block id must match");
        assertEq(wakeupBlock.timeHint, "08:00 AM", "Wakeup time hint must match");

        System.out.println("✅ ScheduleManagerTest: ALL ASSERTIONS PASSED!");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (actual == null ? expected != null : !actual.equals(expected)) {
            throw new AssertionError("FAILED: " + msg + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}

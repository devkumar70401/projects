package com.devendra.healthtracker.routine;

import android.content.Context;
import java.util.List;

/**
 * Public Contract interface for the Routine & Checklist Capability.
 * Mandated by MODULAR_ARCHITECTURE_CONSTITUTION.md (Rule 9).
 */
public interface RoutineContract {

    void scheduleAllAlarms(Context context);

    void cancelAllAlarms(Context context);

    List<ScheduleManager.Phase> getAllPhases(Context context);

    List<ChecklistItem> getChecklistItems(Context context);

    void addChecklistItem(Context context, String title);

    void toggleChecklistItem(Context context, String itemId);

    List<RoutineBlock> getAllRoutineBlocks(Context context);

    boolean isRoutineBlockCompleted(Context context, String blockId);

    boolean toggleRoutineBlock(Context context, String blockId);

    void resetDailyRoutine(Context context);
}

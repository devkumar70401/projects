package com.devendra.healthtracker.wellbeing;

import android.content.Context;
import java.util.List;

/**
 * Public Contract interface for the Digital Wellbeing Capability.
 * Mandated by MODULAR_ARCHITECTURE_CONSTITUTION.md (Rule 9).
 */
public interface WellbeingContract {

    DailyWellbeingRecord getTodayRecord(Context context);

    List<DailyWellbeingRecord> getRecentHistory(Context context, int days);

    void recordFocusMinutes(Context context, int minutes);

    void recordCompletedTask(Context context);
}

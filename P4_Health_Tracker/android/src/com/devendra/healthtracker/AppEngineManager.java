package com.devendra.healthtracker;

import com.devendra.healthtracker.pomodoro.PomodoroManager;
import com.devendra.healthtracker.routine.AlarmAudioService;
import com.devendra.healthtracker.routine.ScheduleManager;

import android.content.Context;
import android.content.SharedPreferences;

public class AppEngineManager {

    private static final String PREFS_NAME = "RoseAppEnginePrefs";
    private static final String KEY_APP_ENABLED = "app_engine_enabled";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Checks if the Rose app engine is active and enabled.
     * Defaults to true so users receive guidance unless explicitly turned off.
     */
    public static boolean isAppEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_APP_ENABLED, true);
    }

    /**
     * Toggles the master app engine state.
     * When disabled, all scheduled alarms and background audio are cancelled.
     * When enabled, all cadence routine alarms are re-scheduled.
     */
    public static void setAppEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_APP_ENABLED, enabled).apply();
        if (enabled) {
            ScheduleManager.scheduleAllAlarms(context);
        } else {
            ScheduleManager.cancelAllAlarms(context);
            AlarmAudioService.stopAlarm(context);
            try {
                if ("RUNNING".equals(PomodoroManager.getStatus(context))) {
                    PomodoroManager.pause(context);
                }
            } catch (Exception ignored) {}
        }
    }
}

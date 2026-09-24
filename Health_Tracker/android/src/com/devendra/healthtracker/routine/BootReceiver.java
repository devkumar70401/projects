package com.devendra.healthtracker.routine;

import com.devendra.healthtracker.AppEngineManager;
import com.devendra.healthtracker.pomodoro.PomodoroManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (AppEngineManager.isAppEnabled(context)) {
                ScheduleManager.scheduleAllAlarms(context);
            }
            // Recover Pomodoro timer state after device restart
            try {
                String pomoStatus = PomodoroManager.getStatus(context);
                if ("RUNNING".equals(pomoStatus)) {
                    int remaining = PomodoroManager.getRemainingSeconds(context);
                    if (remaining <= 0) {
                        PomodoroManager.onSessionComplete(context);
                    } else {
                        // Re-register exact alarm and restart foreground service
                        PomodoroManager.start(context);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}

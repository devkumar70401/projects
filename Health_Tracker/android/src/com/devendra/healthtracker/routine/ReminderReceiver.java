package com.devendra.healthtracker.routine;

import com.devendra.healthtracker.AppEngineManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_STOP_ALARM = "com.devendra.healthtracker.STOP_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        if (ACTION_STOP_ALARM.equals(intent.getAction())) {
            AlarmAudioService.stopAlarm(context);
            return;
        }

        if (!AppEngineManager.isAppEnabled(context)) {
            return;
        }

        String title = intent.getStringExtra("title");
        String roman = intent.getStringExtra("roman");
        String protocol = intent.getStringExtra("protocol");
        String time = intent.getStringExtra("time");

        String phaseId = intent.getStringExtra("phase_id");
        if ("phase-08".equals(phaseId)) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.TUESDAY) {
                title = "Fuel Phase 3: 6 PM Paneer Reset (Tuesday Zero-Egg)";
                protocol = "TUESDAY (ZERO-EGG): 80g–100g Fresh Paneer/Tofu + Steamed Sprouted Moong + Roasted Chana + 1/2 Lemon + 3–5g Creatine in water.";
            }
        }

        // Start reliable foreground alarm service
        AlarmAudioService.startAlarm(context, title, roman, protocol, time);

        // Reschedule alarms for continuous daily cadence
        ScheduleManager.scheduleAllAlarms(context);
    }
}

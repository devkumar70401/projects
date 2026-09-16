package com.devendra.healthtracker.pomodoro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.PowerManager;

/**
 * BroadcastReceiver triggered by AlarmManager exact alarms to guarantee
 * 100% reliable Pomodoro session completion even under Android Doze mode.
 */
public class PomodoroReceiver extends BroadcastReceiver {
    public static final String ACTION_POMODORO_ALARM = "com.devendra.healthtracker.POMODORO_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_POMODORO_ALARM.equals(action)) {
            // Verify if still in running state before finishing
            String status = PomodoroManager.getStatus(context);
            if ("RUNNING".equals(status)) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    try {
                        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Rose:PomodoroReceiverWakeLock");
                        wl.setReferenceCounted(false);
                        wl.acquire(5000); // Hold CPU awake for 5 seconds so alarm chime rings completely
                    } catch (Exception ignored) {}
                }
                PomodoroManager.onSessionComplete(context);
            }
        }
    }
}

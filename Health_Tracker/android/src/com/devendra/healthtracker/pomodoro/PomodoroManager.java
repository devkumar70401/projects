package com.devendra.healthtracker.pomodoro;

import com.devendra.healthtracker.MainActivity;
import com.devendra.healthtracker.R;
import com.devendra.healthtracker.sound.SoundManager;
import com.devendra.healthtracker.wellbeing.WellbeingHistoryManager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PomodoroManager {

    private static final String PREFS_NAME = "RosePomodoroPrefs";
    private static final String KEY_PHASE = "pomo_phase"; // "WORK", "SHORT_BREAK", "LONG_BREAK"
    private static final String KEY_STATUS = "pomo_status"; // "IDLE", "RUNNING", "PAUSED"
    private static final String KEY_TOTAL_SEC = "pomo_total_sec";
    private static final String KEY_REMAINING_SEC = "pomo_remaining_sec";
    private static final String KEY_END_TIME_MS = "pomo_end_time_ms";
    private static final String KEY_COMPLETED_TODAY = "pomo_completed_today";
    private static final String KEY_TOTAL_FOCUS_SEC = "pomo_total_focus_sec";
    private static final String KEY_LAST_DATE = "pomo_last_date";

    public static final int DURATION_WORK_DEFAULT = 25 * 60; // 25 mins
    public static final int DURATION_SHORT_BREAK = 5 * 60;   // 5 mins
    public static final int DURATION_LONG_BREAK = 15 * 60;   // 15 mins
    public static final int DURATION_DEEP_WORK = 50 * 60;    // 50 mins

    public static final String CHANNEL_ALERT_ID = "pomodoro_alert_channel";

    public interface OnPomodoroUpdateListener {
        void onPomodoroUpdate(String phase, String status, int remainingSec, int totalSec, int completedCount);
    }

    private static final List<OnPomodoroUpdateListener> listeners = new ArrayList<>();

    public static synchronized void registerListener(OnPomodoroUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static synchronized void unregisterListener(OnPomodoroUpdateListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void notifyListeners(Context context) {
        String phase = getPhase(context);
        String status = getStatus(context);
        int remaining = getRemainingSeconds(context);
        int total = getTotalSeconds(context);
        int completed = getCompletedToday(context);

        for (OnPomodoroUpdateListener l : new ArrayList<>(listeners)) {
            try {
                l.onPomodoroUpdate(phase, status, remaining, total, completed);
            } catch (Exception ignored) {}
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        checkDateReset(context);
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void checkDateReset(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = sp.getString(KEY_LAST_DATE, "");
        if (!today.equals(lastDate)) {
            sp.edit()
              .putString(KEY_LAST_DATE, today)
              .putInt(KEY_COMPLETED_TODAY, 0)
              .putInt(KEY_TOTAL_FOCUS_SEC, 0)
              .apply();
        }
    }

    public static String getPhase(Context context) {
        return getPrefs(context).getString(KEY_PHASE, "WORK");
    }

    public static String getStatus(Context context) {
        return getPrefs(context).getString(KEY_STATUS, "IDLE");
    }

    public static int getTotalSeconds(Context context) {
        return getPrefs(context).getInt(KEY_TOTAL_SEC, DURATION_WORK_DEFAULT);
    }

    public static int getRemainingSeconds(Context context) {
        SharedPreferences sp = getPrefs(context);
        String status = sp.getString(KEY_STATUS, "IDLE");
        int totalSec = sp.getInt(KEY_TOTAL_SEC, DURATION_WORK_DEFAULT);
        if ("RUNNING".equals(status)) {
            long endTime = sp.getLong(KEY_END_TIME_MS, 0);
            long diff = (endTime - System.currentTimeMillis()) / 1000;
            return Math.min(totalSec, Math.max(0, (int) diff));
        }
        int remaining = sp.getInt(KEY_REMAINING_SEC, DURATION_WORK_DEFAULT);
        return Math.min(totalSec, Math.max(0, remaining));
    }

    public static int getCompletedToday(Context context) {
        return getPrefs(context).getInt(KEY_COMPLETED_TODAY, 0);
    }

    public static int getTotalFocusMinutesToday(Context context) {
        int totalSec = getPrefs(context).getInt(KEY_TOTAL_FOCUS_SEC, 0);
        return totalSec / 60;
    }

    private static void scheduleExactAlarm(Context context, long triggerAtMillis) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(context, PomodoroReceiver.class);
            intent.setAction(PomodoroReceiver.ACTION_POMODORO_ALARM);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(context, 4001, intent, flags);

            boolean canExact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canExact = am.canScheduleExactAlarms();
            }

            try {
                if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
            } catch (SecurityException se) {
                // Defensive fallback when exact alarm permission is denied
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void cancelExactAlarm(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(context, PomodoroReceiver.class);
            intent.setAction(PomodoroReceiver.ACTION_POMODORO_ALARM);
            PendingIntent pi = PendingIntent.getBroadcast(
                context,
                4001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            am.cancel(pi);
        } catch (Exception ignored) {}
    }

    public static synchronized void start(Context context) {
        SoundManager.stopActiveRingtone();
        int remaining = getRemainingSeconds(context);
        if (remaining <= 0) {
            remaining = getTotalSeconds(context);
        }
        long endTime = System.currentTimeMillis() + (remaining * 1000L);

        getPrefs(context).edit()
            .putString(KEY_STATUS, "RUNNING")
            .putInt(KEY_REMAINING_SEC, remaining)
            .putLong(KEY_END_TIME_MS, endTime)
            .apply();

        scheduleExactAlarm(context, endTime);
        PomodoroService.startService(context);
        updateAllWidgets(context);
        notifyListeners(context);
    }

    public static synchronized void pause(Context context) {
        SoundManager.stopActiveRingtone();
        int remaining = getRemainingSeconds(context);

        cancelExactAlarm(context);
        getPrefs(context).edit()
            .putString(KEY_STATUS, "PAUSED")
            .putInt(KEY_REMAINING_SEC, remaining)
            .putLong(KEY_END_TIME_MS, 0)
            .apply();

        PomodoroService.startService(context); // Update notification to paused state
        updateAllWidgets(context);
        notifyListeners(context);
    }

    public static synchronized void reset(Context context) {
        SoundManager.stopActiveRingtone();
        int total = getTotalSeconds(context);

        cancelExactAlarm(context);
        getPrefs(context).edit()
            .putString(KEY_STATUS, "IDLE")
            .putInt(KEY_REMAINING_SEC, total)
            .putLong(KEY_END_TIME_MS, 0)
            .apply();

        PomodoroService.stopService(context);
        updateAllWidgets(context);
        notifyListeners(context);
    }

    public static synchronized void setPreset(Context context, String phase, int durationSec) {
        SoundManager.stopActiveRingtone();
        cancelExactAlarm(context);
        getPrefs(context).edit()
            .putString(KEY_PHASE, phase)
            .putString(KEY_STATUS, "IDLE")
            .putInt(KEY_TOTAL_SEC, durationSec)
            .putInt(KEY_REMAINING_SEC, durationSec)
            .putLong(KEY_END_TIME_MS, 0)
            .apply();

        PomodoroService.stopService(context);
        updateAllWidgets(context);
        notifyListeners(context);
    }

    public static synchronized void skip(Context context) {
        SoundManager.stopActiveRingtone();
        cancelExactAlarm(context);
        String currentPhase = getPhase(context);
        if ("WORK".equals(currentPhase)) {
            // Check completed count
            int completed = getCompletedToday(context);
            if ((completed + 1) % 4 == 0) {
                setPreset(context, "LONG_BREAK", DURATION_LONG_BREAK);
            } else {
                setPreset(context, "SHORT_BREAK", DURATION_SHORT_BREAK);
            }
        } else {
            setPreset(context, "WORK", DURATION_WORK_DEFAULT);
        }
    }

    public static synchronized void toggle(Context context) {
        String status = getStatus(context);
        if ("RUNNING".equals(status)) {
            pause(context);
        } else {
            start(context);
        }
    }

    public static synchronized void onSessionComplete(Context context) {
        cancelExactAlarm(context);
        SharedPreferences sp = getPrefs(context);
        String status = sp.getString(KEY_STATUS, "IDLE");
        if (!"RUNNING".equals(status)) {
            return; // Guard against concurrent duplicate invocation
        }
        String phase = sp.getString(KEY_PHASE, "WORK");
        int completed = sp.getInt(KEY_COMPLETED_TODAY, 0);
        int totalFocusSec = sp.getInt(KEY_TOTAL_FOCUS_SEC, 0);
        int currentTotalSec = sp.getInt(KEY_TOTAL_SEC, DURATION_WORK_DEFAULT);

        String alertTitle;
        String alertMsg;
        String nextPhase;
        int nextDuration;

        if ("WORK".equals(phase)) {
            completed++;
            totalFocusSec += currentTotalSec;
            alertTitle = "🍅 Focus Session Complete!";
            if (completed % 4 == 0) {
                alertMsg = "You've finished 4 focus blocks! Take a well-earned 15-minute Long Break.";
                nextPhase = "LONG_BREAK";
                nextDuration = DURATION_LONG_BREAK;
            } else {
                alertMsg = "Great flow state! Take a 5-minute Short Break to hydrate.";
                nextPhase = "SHORT_BREAK";
                nextDuration = DURATION_SHORT_BREAK;
            }
            // Multi-Day Digital Wellbeing Telemetry Integration
            int focusMinutes = Math.max(1, currentTotalSec / 60);
            WellbeingHistoryManager.recordFocusSession(context, focusMinutes);
        } else {
            alertTitle = "☕ Break Complete!";
            alertMsg = "Ready for the next deep work block. Tap Start to begin.";
            nextPhase = "WORK";
            nextDuration = DURATION_WORK_DEFAULT;
        }

        sp.edit()
          .putString(KEY_PHASE, nextPhase)
          .putString(KEY_STATUS, "IDLE")
          .putInt(KEY_TOTAL_SEC, nextDuration)
          .putInt(KEY_REMAINING_SEC, nextDuration)
          .putLong(KEY_END_TIME_MS, 0)
          .putInt(KEY_COMPLETED_TODAY, completed)
          .putInt(KEY_TOTAL_FOCUS_SEC, totalFocusSec)
          .apply();

        // Fire alert chime & notification
        fireCompletionAlert(context, alertTitle, alertMsg);

        PomodoroService.stopService(context);
        updateAllWidgets(context);
        notifyListeners(context);
    }

    private static void fireCompletionAlert(Context context, String title, String message) {
        // 1. Play Completion Chime / Ringtone if sound toggle is enabled
        SoundManager.playCompletionChime(context);

        // 2. Fire Push Notification if notification toggle is enabled
        if (!SoundManager.isNotificationEnabled(context)) {
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_ALERT_ID,
                "Pomodoro Completion Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Audio and vibration alert when a Pomodoro interval ends");
            alertChannel.enableVibration(true);
            alertChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            alertChannel.setLightColor(Color.parseColor("#E11D48"));
            alertChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Configure audio attributes on channel
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri != null) {
                AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
                alertChannel.setSound(soundUri, aa);
            }

            nm.createNotificationChannel(alertChannel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("openPage", 2); // Pomodoro is Card 2
        openIntent.putExtra("targetTab", "pomodoro");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            context,
            3002,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ALERT_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(R.drawable.app_logo)
               .setContentTitle(title)
               .setContentText(message)
               .setContentIntent(pi)
               .setAutoCancel(true)
               .setPriority(Notification.PRIORITY_HIGH)
               .setDefaults(Notification.DEFAULT_ALL);

        nm.notify(3002, builder.build());

        // Haptic pulse
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(new long[]{0, 500, 200, 500}, -1);
            }
        } catch (Exception ignored) {}
    }

    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, PomodoroWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(widget);
            if (ids != null && ids.length > 0) {
                Intent updateIntent = new Intent(context, PomodoroWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(updateIntent);
            }
        } catch (Exception ignored) {}
    }

    public static String formatTime(int totalSeconds) {
        int m = Math.max(0, totalSeconds) / 60;
        int s = Math.max(0, totalSeconds) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
}

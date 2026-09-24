package com.devendra.healthtracker.pomodoro;

import com.devendra.healthtracker.MainActivity;
import com.devendra.healthtracker.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

public class PomodoroWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TOGGLE = "com.devendra.healthtracker.ACTION_TOGGLE_POMODORO";
    public static final String ACTION_RESET  = "com.devendra.healthtracker.ACTION_RESET_POMODORO";
    public static final String ACTION_SKIP   = "com.devendra.healthtracker.ACTION_SKIP_POMODORO";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String phase = PomodoroManager.getPhase(context);
        String status = PomodoroManager.getStatus(context);
        int remaining = PomodoroManager.getRemainingSeconds(context);
        int total = PomodoroManager.getTotalSeconds(context);
        int completed = PomodoroManager.getCompletedToday(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_pomodoro);

        // 1. Timer Text
        views.setTextViewText(R.id.widget_timer_text, PomodoroManager.formatTime(remaining));

        // 2. Phase Badge Text
        String badgeText = "WORK".equals(phase) ? "FOCUS" : ("LONG_BREAK".equals(phase) ? "LONG BREAK" : "BREAK");
        views.setTextViewText(R.id.widget_phase_badge, badgeText);

        // 3. Session dots / count
        int sessionInCycle = (completed % 4) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            if (i < sessionInCycle) {
                sb.append("🍅 ");
            } else if (i == sessionInCycle) {
                sb.append("🎯 ");
            } else {
                sb.append("⚪ ");
            }
        }
        views.setTextViewText(R.id.widget_session_text, "Session " + sessionInCycle + " / 4  ·  " + sb.toString().trim());

        // 4. Progress Bar
        int progress = total > 0 ? (int) (((long) (total - remaining) * 100) / total) : 0;
        views.setProgressBar(R.id.widget_progress, 100, progress, false);

        // 5. Toggle Button text
        boolean isRunning = "RUNNING".equals(status);
        views.setTextViewText(R.id.widget_btn_toggle, isRunning ? "⏸ Pause" : "▶ Start");

        // 6. Setup Click Intents
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        // Root Click -> Open Rose app to Pomodoro tab (Page 2)
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.putExtra("openPage", 2);
        openAppIntent.putExtra("targetTab", "pomodoro");
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, 401, openAppIntent, piFlags);
        views.setOnClickPendingIntent(R.id.widget_root, openPi);

        // Toggle Click
        Intent toggleIntent = new Intent(context, PomodoroWidgetProvider.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getBroadcast(context, 402, toggleIntent, piFlags);
        views.setOnClickPendingIntent(R.id.widget_btn_toggle, togglePi);

        // Reset Click
        Intent resetIntent = new Intent(context, PomodoroWidgetProvider.class);
        resetIntent.setAction(ACTION_RESET);
        PendingIntent resetPi = PendingIntent.getBroadcast(context, 403, resetIntent, piFlags);
        views.setOnClickPendingIntent(R.id.widget_btn_reset, resetPi);

        // Skip Click
        Intent skipIntent = new Intent(context, PomodoroWidgetProvider.class);
        skipIntent.setAction(ACTION_SKIP);
        PendingIntent skipPi = PendingIntent.getBroadcast(context, 404, skipIntent, piFlags);
        views.setOnClickPendingIntent(R.id.widget_btn_skip, skipPi);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null) return;

        String action = intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            PomodoroManager.toggle(context);
        } else if (ACTION_RESET.equals(action)) {
            PomodoroManager.reset(context);
        } else if (ACTION_SKIP.equals(action)) {
            PomodoroManager.skip(context);
        }

        // Force update all widgets
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, PomodoroWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(cn);
        if (ids != null) {
            for (int id : ids) {
                updateAppWidget(context, manager, id);
            }
        }
    }
}

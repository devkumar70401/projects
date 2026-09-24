package com.devendra.healthtracker.pomodoro;

import com.devendra.healthtracker.MainActivity;
import com.devendra.healthtracker.R;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class PomodoroService extends Service {

    public static final String ACTION_START = "com.devendra.healthtracker.action.POMODORO_START";
    public static final String ACTION_PAUSE = "com.devendra.healthtracker.action.POMODORO_PAUSE";
    public static final String ACTION_RESET = "com.devendra.healthtracker.action.POMODORO_RESET";
    public static final String ACTION_SKIP  = "com.devendra.healthtracker.action.POMODORO_SKIP";
    public static final String ACTION_STOP  = "com.devendra.healthtracker.action.POMODORO_STOP";

    public static final String CHANNEL_ID = "pomodoro_timer_channel";
    public static final int NOTIFICATION_ID = 3001;

    private Handler tickerHandler;
    private Runnable tickerRunnable;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private int tickCount = 0;

    public static void startService(Context context) {
        Intent intent = new Intent(context, PomodoroService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, PomodoroService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        tickerHandler = new Handler(Looper.getMainLooper());

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Rose:PomodoroWakeLock");
            wakeLock.setReferenceCounted(false);
        }

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Persistent live countdown for Pomodoro sessions");
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action) || ACTION_RESET.equals(action)) {
            if (ACTION_RESET.equals(action)) {
                PomodoroManager.reset(this);
            }
            stopTickerAndFinish();
            return START_NOT_STICKY;
        } else if (ACTION_PAUSE.equals(action)) {
            PomodoroManager.pause(this);
            stopTicker();
            updateNotification();
            return START_STICKY;
        } else if (ACTION_SKIP.equals(action)) {
            PomodoroManager.skip(this);
            updateNotification();
            return START_STICKY;
        }

        // Start / Resume
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(1000 * 60 * 60); // 1 hour max
        }
        startTicker();

        return START_STICKY;
    }

    private void startTicker() {
        stopTicker();
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                tickCount++;
                String status = PomodoroManager.getStatus(PomodoroService.this);
                if (!"RUNNING".equals(status)) {
                    stopTicker();
                    return;
                }

                int remaining = PomodoroManager.getRemainingSeconds(PomodoroService.this);
                if (remaining <= 0) {
                    PomodoroManager.onSessionComplete(PomodoroService.this);
                    stopTickerAndFinish();
                    return;
                }

                // Update notification every 1 second
                updateNotification();
                PomodoroManager.notifyListeners(PomodoroService.this);

                // Update widgets every 5 seconds or when last minute
                if (tickCount % 5 == 0 || remaining < 60) {
                    PomodoroManager.updateAllWidgets(PomodoroService.this);
                }

                tickerHandler.postDelayed(this, 1000);
            }
        };
        tickerHandler.post(tickerRunnable);
    }

    private void stopTicker() {
        if (tickerHandler != null && tickerRunnable != null) {
            tickerHandler.removeCallbacks(tickerRunnable);
            tickerRunnable = null;
        }
    }

    private void updateNotification() {
        if (notificationManager != null) {
            try {
                notificationManager.notify(NOTIFICATION_ID, buildNotification());
            } catch (Exception ignored) {}
        }
    }

    private Notification buildNotification() {
        String phase = PomodoroManager.getPhase(this);
        String status = PomodoroManager.getStatus(this);
        int remaining = PomodoroManager.getRemainingSeconds(this);
        int total = PomodoroManager.getTotalSeconds(this);
        int completed = PomodoroManager.getCompletedToday(this);

        String timeStr = PomodoroManager.formatTime(remaining);
        String phaseLabel = "WORK".equals(phase) ? "🍅 Focus Session" : ("LONG_BREAK".equals(phase) ? "🌴 Long Break" : "☕ Short Break");
        String title = phaseLabel + " — " + timeStr;
        String content = "Session " + ((completed % 4) + 1) + " / 4 · " + completed + " completed today";

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.putExtra("openPage", 2);
        openIntent.putExtra("targetTab", "pomodoro");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(
            this,
            3001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.app_logo)
               .setContentTitle(title)
               .setContentText(content)
               .setContentIntent(openPi)
               .setOngoing("RUNNING".equals(status))
               .setAutoCancel(false)
               .setOnlyAlertOnce(true)
               .setProgress(total, total - remaining, false);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        if ("RUNNING".equals(status)) {
            Intent pauseIntent = new Intent(this, PomodoroService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pausePi = PendingIntent.getService(this, 301, pauseIntent, piFlags);
            builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_pause, "⏸ Pause", pausePi
            ).build());
        } else {
            Intent resumeIntent = new Intent(this, PomodoroService.class);
            resumeIntent.setAction(ACTION_START);
            PendingIntent resumePi = PendingIntent.getService(this, 302, resumeIntent, piFlags);
            builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_play, "▶ Resume", resumePi
            ).build());
        }

        Intent skipIntent = new Intent(this, PomodoroService.class);
        skipIntent.setAction(ACTION_SKIP);
        PendingIntent skipPi = PendingIntent.getService(this, 303, skipIntent, piFlags);
        builder.addAction(new Notification.Action.Builder(
            android.R.drawable.ic_media_next, "⏭ Skip", skipPi
        ).build());

        Intent resetIntent = new Intent(this, PomodoroService.class);
        resetIntent.setAction(ACTION_RESET);
        PendingIntent resetPi = PendingIntent.getService(this, 304, resetIntent, piFlags);
        builder.addAction(new Notification.Action.Builder(
            android.R.drawable.ic_menu_revert, "↺ Reset", resetPi
        ).build());

        return builder.build();
    }

    private void stopTickerAndFinish() {
        stopTicker();
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {}
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopTickerAndFinish();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

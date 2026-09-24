package com.devendra.healthtracker.downloader;

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
import android.os.Build;
import android.os.IBinder;

public class MediaDownloadService extends Service {
    public static final String CHANNEL_ID = "rose_downloads_channel";
    public static final int NOTIFICATION_ID = 4040;
    public static final String ACTION_CANCEL = "com.devendra.healthtracker.ACTION_CANCEL_DOWNLOAD";
    public static final String EXTRA_JOB_ID = "extra_job_id";

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            String jobId = intent.getStringExtra(EXTRA_JOB_ID);
            if (jobId != null) {
                MediaDownloadManager.getInstance(this).cancelJob(jobId);
            }
        }

        Notification notification = buildForegroundNotification("Downloading Media", 0, "Initializing...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= 34) { // Android 14+
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void updateProgress(String title, int progressPercent, String speedText) {
        if (notificationManager == null) return;
        Notification notif = buildForegroundNotification(title, progressPercent, speedText);
        notificationManager.notify(NOTIFICATION_ID, notif);
    }

    public void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }

    private Notification buildForegroundNotification(String title, int progressPercent, String detailText) {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.putExtra("openPage", 8); // Open Card 8
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(title)
                .setContentText(detailText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (progressPercent >= 0) {
            builder.setProgress(100, progressPercent, progressPercent == 0);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Downloads",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active background download progress and speed");
            channel.setShowBadge(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}

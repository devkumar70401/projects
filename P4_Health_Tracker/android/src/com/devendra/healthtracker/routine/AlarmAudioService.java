package com.devendra.healthtracker.routine;

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
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class AlarmAudioService extends Service {

    public static final String ACTION_PLAY = "com.devendra.healthtracker.action.PLAY";
    public static final String ACTION_STOP = "com.devendra.healthtracker.action.STOP";
    public static final String CHANNEL_ID = "health_cadence_foreground_channel";
    public static final int NOTIFICATION_ID = 2001;

    private static volatile boolean isServiceRunning = false;

    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    public static void startAlarm(Context context, String title, String roman, String protocol, String time) {
        Intent intent = new Intent(context, AlarmAudioService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra("title", title);
        intent.putExtra("roman", roman);
        intent.putExtra("protocol", protocol);
        intent.putExtra("time", time);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopAlarm(Context context) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Exception ignored) {}
        Intent intent = new Intent(context, AlarmAudioService.class);
        intent.setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Exception ignored) {}
    }

    public static boolean isRunning() {
        return isServiceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HealthTracker:AlarmAudioWakeLock");
            wakeLock.setReferenceCounted(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopPlaybackAndFinish();
            return START_NOT_STICKY;
        }

        String title = intent.getStringExtra("title");
        String roman = intent.getStringExtra("roman");
        String protocol = intent.getStringExtra("protocol");
        String time = intent.getStringExtra("time");

        if (title == null) title = "Cadence Boundary";
        if (roman == null) roman = "CADENCE ALERT";

        // 1. Build Notification Channel with SILENT system sound so System UI does not conflict
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Cadence Alarms & Audio Playback",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Full-fidelity alarm audio and stop controls");
            channel.setSound(null, null); // Disable system notification beep; audio handled by Service!
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 400, 200, 400});
            channel.setLightColor(Color.CYAN);
            nm.createNotificationChannel(channel);
        }

        // 2. Open Activity Pending Intent
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent openPi = PendingIntent.getActivity(this, 100, openIntent, piFlags);

        // 3. Stop Song Action Pending Intent (Sends ACTION_STOP to this Service)
        Intent stopIntent = new Intent(this, AlarmAudioService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 101, stopIntent, piFlags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
               .setContentTitle("🎵 " + roman + " — " + title)
               .setContentText(protocol != null ? protocol : "Playing Summertime Sadness (Lana Del Rey)")
               .setContentIntent(openPi)
               .setOngoing(true)
               .setAutoCancel(false)
               .setPriority(Notification.PRIORITY_HIGH)
               .addAction(new Notification.Action.Builder(
                   android.R.drawable.ic_media_pause,
                   "⏹ Stop Song",
                   stopPi
               ).build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        if (protocol != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setStyle(new Notification.BigTextStyle()
                .setBigContentTitle("🎵 " + roman + " · " + title)
                .setSummaryText(time != null ? time : "Active Interval")
                .bigText("Now Playing: Lana Del Rey — Summertime Sadness (Ringtone)\n\n" + protocol));
        }

        Notification notification = builder.build();

        // 4. Start Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 5. Keep CPU awake while song plays
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(180000); // 3 minutes max
        }

        // 6. Request Audio Focus so pulling notification shade will NEVER pause or duck
        requestAudioFocus();

        // 7. Start MediaPlayer
        startMediaPlayback();

        return START_NOT_STICKY;
    }

    private void requestAudioFocus() {
        if (audioManager == null) return;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false) // Do NOT pause or duck when notification shade opens!
                .setOnAudioFocusChangeListener(focusChange -> {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopPlaybackAndFinish();
                    }
                })
                .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(
                focusChange -> {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopPlaybackAndFinish();
                    }
                },
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            );
        }
    }

    private void startMediaPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.summertime_sadness);
            if (mediaPlayer == null) return;

            AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
            mediaPlayer.setAudioAttributes(attrs);
            mediaPlayer.setLooping(true);

            mediaPlayer.setOnCompletionListener(mp -> {
                // When song finishes naturally, stop service
                stopPlaybackAndFinish();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopPlaybackAndFinish();
                return true;
            });

            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            stopPlaybackAndFinish();
        }
    }

    private void stopPlaybackAndFinish() {
        isServiceRunning = false;

        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {}
        }

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        if (audioManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                } else {
                    audioManager.abandonAudioFocus(null);
                }
            } catch (Exception ignored) {}
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder dummy = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Stopping...");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, dummy.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } else {
                    startForeground(NOTIFICATION_ID, dummy.build());
                }
            }
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID);
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopPlaybackAndFinish();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

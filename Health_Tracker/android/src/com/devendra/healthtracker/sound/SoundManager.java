package com.devendra.healthtracker.sound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;

public class SoundManager {

    private static final String PREFS_NAME = "RoseSoundPrefs";
    private static final String KEY_SOUND_ENABLED = "sound_alert_enabled";
    private static final String KEY_NOTIF_ENABLED = "notification_alert_enabled";

    private static Ringtone activeRingtone = null;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isSoundEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public static boolean isNotificationEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_NOTIF_ENABLED, true);
    }

    public static void setNotificationEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply();
    }

    /**
     * Plays completion chime if sound alerts are enabled.
     * Uses RingtoneManager with USAGE_ALARM, and backs up with ToneGenerator hardware synthesis.
     */
    public static synchronized void playCompletionChime(Context context) {
        if (!isSoundEnabled(context)) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        boolean ringtonePlayed = false;

        // 1. Play System Alarm / Ringtone (Alarm stream breaks through DND and vibration)
        try {
            Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            if (alertUri != null) {
                if (activeRingtone != null && activeRingtone.isPlaying()) {
                    activeRingtone.stop();
                }
                activeRingtone = RingtoneManager.getRingtone(appContext, alertUri);
                if (activeRingtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes aa = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                        activeRingtone.setAudioAttributes(aa);
                    } else {
                        activeRingtone.setStreamType(AudioManager.STREAM_ALARM);
                    }
                    activeRingtone.play();
                    ringtonePlayed = true;

                    // Auto-cutoff after 4 seconds so system alarm ringtones do not loop indefinitely
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        SoundManager::stopActiveRingtone,
                        4000
                    );
                }
            }
        } catch (Exception ignored) {}

        // 2. Hardware ToneGenerator Fallback (Double-Chime Ramp) if ringtone failed or unavailable
        if (!ringtonePlayed) {
            new Thread(() -> {
                try {
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350);
                    Thread.sleep(400);
                    tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 500);
                    Thread.sleep(550);
                    tg.release();
                } catch (Exception ignored) {}
            }).start();
        }
    }

    /**
     * Short confirmation preview tone when user enables sound toggle.
     */
    public static void previewChime(Context context) {
        if (!isSoundEnabled(context)) {
            return;
        }
        new Thread(() -> {
            try {
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                Thread.sleep(200);
                tg.release();
            } catch (Exception ignored) {}
        }).start();
    }

    public static synchronized void stopActiveRingtone() {
        if (activeRingtone != null && activeRingtone.isPlaying()) {
            try {
                activeRingtone.stop();
            } catch (Exception ignored) {}
            activeRingtone = null;
        }
    }
}

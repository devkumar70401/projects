package com.devendra.healthtracker.biometrics;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BiometricsManager {

    private static final String PREFS_NAME = "health_tracker_biometrics";
    public static final int DAILY_WATER_GOAL_ML = 3000;

    public static String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static int getWaterIntakeMl(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastDate = sp.getString("water_date", "");
        String today = getTodayKey();
        if (!today.equals(lastDate)) {
            // New day: reset
            sp.edit().putString("water_date", today).putInt("water_ml", 1250).apply();
            return 1250;
        }
        return sp.getInt("water_ml", 1250);
    }

    public static void addWater(Context context, int deltaMl) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int current = getWaterIntakeMl(context);
        int updated = Math.max(0, Math.min(6000, current + deltaMl));
        sp.edit().putString("water_date", getTodayKey()).putInt("water_ml", updated).apply();
    }

    public static boolean isMacroChecked(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedDate = sp.getString("date_" + key, "");
        if (!getTodayKey().equals(savedDate)) {
            return false;
        }
        return sp.getBoolean("val_" + key, false);
    }

    public static void setMacroChecked(Context context, String key, boolean checked) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString("date_" + key, getTodayKey()).putBoolean("val_" + key, checked).apply();
    }
}

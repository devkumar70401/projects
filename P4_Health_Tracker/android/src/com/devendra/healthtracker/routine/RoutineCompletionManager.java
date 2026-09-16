package com.devendra.healthtracker.routine;

import com.devendra.healthtracker.wellbeing.WellbeingHistoryManager;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages daily completion state and tickmarking for all routine blocks and ingredients.
 * State automatically tracks by calendar date so each morning starts fresh,
 * while history is recorded into wellbeing telemetry.
 */
public class RoutineCompletionManager {

    private static final String PREF_NAME = "RoseRoutineCompletionPrefs";

    private static SharedPreferences getPrefs(Context context) {
        if (context == null) return null;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String getTodayKey(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(timestamp));
    }

    private static String makeKey(String todayKey, String blockId) {
        return todayKey + "_" + blockId;
    }

    public static synchronized boolean isBlockCompleted(Context context, String blockId) {
        if (context == null || blockId == null || blockId.isEmpty()) return false;
        SharedPreferences sp = getPrefs(context);
        if (sp == null) return false;
        return sp.getBoolean(makeKey(getTodayKey(), blockId), false);
    }

    public static synchronized boolean isBlockCompleted(Context context, String todayKey, String blockId) {
        if (context == null || blockId == null || blockId.isEmpty()) return false;
        SharedPreferences sp = getPrefs(context);
        if (sp == null) return false;
        return sp.getBoolean(makeKey(todayKey, blockId), false);
    }

    public static synchronized boolean toggleBlock(Context context, String blockId) {
        if (context == null || blockId == null || blockId.isEmpty()) return false;
        boolean current = isBlockCompleted(context, blockId);
        boolean target = !current;
        setBlockCompleted(context, blockId, target);
        return target;
    }

    public static synchronized void setBlockCompleted(Context context, String blockId, boolean completed) {
        if (context == null || blockId == null || blockId.isEmpty()) return;
        SharedPreferences sp = getPrefs(context);
        if (sp == null) return;

        String today = getTodayKey();
        String key = makeKey(today, blockId);
        String tsKey = key + "_ts";

        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, completed);
        if (completed) {
            editor.putLong(tsKey, System.currentTimeMillis());
            try {
                WellbeingHistoryManager.recordTaskCompleted(context);
            } catch (Throwable ignored) {}
        } else {
            editor.remove(tsKey);
            try {
                WellbeingHistoryManager.recordTaskUncompleted(context);
            } catch (Throwable ignored) {}
        }
        editor.apply();
    }

    public static synchronized int getCompletedCount(Context context, List<RoutineBlock> blocks) {
        if (context == null || blocks == null) return 0;
        int count = 0;
        String today = getTodayKey();
        for (RoutineBlock b : blocks) {
            if (b != null && isBlockCompleted(context, today, b.id)) {
                count++;
            }
        }
        return count;
    }

    public static synchronized int getCompletionPercentage(Context context, List<RoutineBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) return 0;
        int completed = getCompletedCount(context, blocks);
        return (completed * 100) / blocks.size();
    }

    public static synchronized void resetToday(Context context) {
        if (context == null) return;
        SharedPreferences sp = getPrefs(context);
        if (sp == null) return;

        String todayPrefix = getTodayKey() + "_";
        SharedPreferences.Editor editor = sp.edit();
        Map<String, ?> all = sp.getAll();
        for (String k : all.keySet()) {
            if (k.startsWith(todayPrefix)) {
                editor.remove(k);
            }
        }
        editor.apply();
    }

    public static synchronized int importBlocksToChecklist(Context context, List<RoutineBlock> blocks) {
        if (context == null || blocks == null || blocks.isEmpty()) return 0;
        List<ChecklistItem> existing = ChecklistManager.getItems(context);
        int added = 0;
        for (RoutineBlock b : blocks) {
            if (b == null) continue;
            boolean alreadyExists = false;
            for (ChecklistItem item : existing) {
                if (item != null && item.title != null && item.title.contains(b.title)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                ChecklistManager.addItem(context, b.title + (b.detail.isEmpty() ? "" : " (" + b.detail + ")"));
                added++;
            }
        }
        return added;
    }
}

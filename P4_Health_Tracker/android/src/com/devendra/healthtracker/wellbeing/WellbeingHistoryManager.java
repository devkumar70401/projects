package com.devendra.healthtracker.wellbeing;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class WellbeingHistoryManager {

    private static final String PREFS_NAME = "RoseWellbeingHistoryPrefs";
    private static final String KEY_RECORDS_MAP = "daily_records_json_map";
    private static final int RETENTION_DAYS = 30;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static synchronized JSONObject loadRecordsMap(Context context) {
        SharedPreferences sp = getPrefs(context);
        String raw = sp.getString(KEY_RECORDS_MAP, "{}");
        try {
            return new JSONObject(raw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static synchronized void saveRecordsMap(Context context, JSONObject map) {
        pruneOldRecords(map, RETENTION_DAYS);
        getPrefs(context).edit().putString(KEY_RECORDS_MAP, map.toString()).apply();
    }

    public static synchronized DailyWellbeingRecord getRecord(Context context, String date) {
        if (date == null || date.isEmpty()) {
            date = getTodayDateString();
        }
        JSONObject map = loadRecordsMap(context);
        if (map.has(date)) {
            try {
                JSONObject obj = map.getJSONObject(date);
                DailyWellbeingRecord rec = DailyWellbeingRecord.fromJson(obj);
                if (rec != null) return rec;
            } catch (Exception ignored) {}
        }
        return new DailyWellbeingRecord(date);
    }

    public static synchronized DailyWellbeingRecord getTodayRecord(Context context) {
        return getRecord(context, getTodayDateString());
    }

    public static synchronized void saveRecord(Context context, DailyWellbeingRecord record) {
        if (record == null || record.date == null || record.date.isEmpty()) return;
        JSONObject map = loadRecordsMap(context);
        try {
            map.put(record.date, record.toJson());
            saveRecordsMap(context, map);
        } catch (JSONException ignored) {}
    }

    public static synchronized void recordFocusSession(Context context, int completedMinutes) {
        String today = getTodayDateString();
        DailyWellbeingRecord rec = getRecord(context, today);
        rec.totalFocusMinutes += Math.max(0, completedMinutes);
        rec.completedPomodoros += 1;
        rec.lastActiveTimestamp = System.currentTimeMillis();
        saveRecord(context, rec);
    }

    public static synchronized void recordTaskCompleted(Context context) {
        String today = getTodayDateString();
        DailyWellbeingRecord rec = getRecord(context, today);
        rec.tasksCompleted += 1;
        rec.lastActiveTimestamp = System.currentTimeMillis();
        saveRecord(context, rec);
    }

    public static synchronized void recordTaskUncompleted(Context context) {
        String today = getTodayDateString();
        DailyWellbeingRecord rec = getRecord(context, today);
        if (rec.tasksCompleted > 0) {
            rec.tasksCompleted -= 1;
        }
        rec.lastActiveTimestamp = System.currentTimeMillis();
        saveRecord(context, rec);
    }

    public static synchronized void recordActiveAppTime(Context context, int minutes) {
        if (minutes <= 0) return;
        String today = getTodayDateString();
        DailyWellbeingRecord rec = getRecord(context, today);
        rec.activeAppMinutes += minutes;
        rec.lastActiveTimestamp = System.currentTimeMillis();
        saveRecord(context, rec);
    }

    /**
     * Returns chronologically ordered list of records for the past N calendar days ending today.
     * Guaranteed to return exactly count items (zero-filled for days with no activity).
     */
    public static synchronized List<DailyWellbeingRecord> getPastDays(Context context, int count) {
        List<DailyWellbeingRecord> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();

        // Start from (today - (count - 1)) days ago
        cal.add(Calendar.DAY_OF_YEAR, -(count - 1));

        for (int i = 0; i < count; i++) {
            String dateStr = sdf.format(cal.getTime());
            list.add(getRecord(context, dateStr));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return list;
    }

    private static void pruneOldRecords(JSONObject map, int keepDays) {
        if (map == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DAY_OF_YEAR, -keepDays);
        Date cutoffDate = cutoff.getTime();

        List<String> keysToRemove = new ArrayList<>();
        Iterator<String> keys = map.keys();
        while (keys.hasNext()) {
            String dateKey = keys.next();
            try {
                Date d = sdf.parse(dateKey);
                if (d != null && d.before(cutoffDate)) {
                    keysToRemove.add(dateKey);
                }
            } catch (Exception e) {
                // Remove malformed keys
                keysToRemove.add(dateKey);
            }
        }

        for (String k : keysToRemove) {
            map.remove(k);
        }
    }
}

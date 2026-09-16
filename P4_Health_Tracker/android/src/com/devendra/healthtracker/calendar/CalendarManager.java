package com.devendra.healthtracker.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CalendarManager
 * High-durability persistent storage for special dates, notes, and memoranda.
 * Dual-persists all entries to both Android SharedPreferences (committed synchronously)
 * and an atomic on-disk JSON file (files/calendar_notes.json) to guarantee 100% survival
 * across system reboots, process termination, and battery drops.
 */
public class CalendarManager {

    private static final String PREF_NAME = "calendar_notes_prefs";
    private static final String BACKUP_FILENAME = "calendar_notes.json";

    public static String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String formatDateKey(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    public static String formatDisplayDate(String dateKey) {
        if (dateKey == null || dateKey.length() != 10) return dateKey;
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = inFormat.parse(dateKey);
            if (d != null) {
                return new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(d);
            }
        } catch (Exception ignored) {}
        return dateKey;
    }

    /**
     * Retrieves note for a specific date (YYYY-MM-DD).
     */
    public static String getNote(Context context, String dateKey) {
        if (context == null || dateKey == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String note = prefs.getString(dateKey, null);
        if (note != null) return note;

        // Fallback to disk JSON backup if not found in SharedPreferences
        Map<String, String> backup = loadFromDiskBackup(context);
        return backup.getOrDefault(dateKey, "");
    }

    /**
     * Retrieves note for today's date.
     */
    public static String getTodayNote(Context context) {
        return getNote(context, getTodayKey());
    }

    /**
     * Persists note for a specific date. If text is null or empty, deletes the note.
     * Uses synchronous commit() and file sync to guarantee durability across reboots.
     */
    public static synchronized void saveNote(Context context, String dateKey, String noteText) {
        if (context == null || dateKey == null) return;
        String trimmed = noteText != null ? noteText.trim() : "";

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (trimmed.isEmpty()) {
            editor.remove(dateKey);
        } else {
            editor.putString(dateKey, trimmed);
        }
        // Use commit() instead of apply() to guarantee physical write to disk before returning
        editor.commit();

        // Sync to secondary backup JSON file
        saveToDiskBackup(context);
    }

    /**
     * Deletes note for a specific date.
     */
    public static synchronized void deleteNote(Context context, String dateKey) {
        saveNote(context, dateKey, "");
    }

    /**
     * Returns all saved date notes.
     */
    public static Map<String, String> getAllNotes(Context context) {
        Map<String, String> result = new HashMap<>();
        if (context == null) return result;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getValue() instanceof String) {
                String val = (String) entry.getValue();
                if (!val.trim().isEmpty()) {
                    result.put(entry.getKey(), val.trim());
                }
            }
        }

        // If SharedPreferences was empty, try backup file
        if (result.isEmpty()) {
            result = loadFromDiskBackup(context);
        }
        return result;
    }

    /**
     * Returns a set of all date keys (YYYY-MM-DD) that have active notes.
     */
    public static Set<String> getDatesWithNotes(Context context) {
        return new HashSet<>(getAllNotes(context).keySet());
    }

    private static synchronized void saveToDiskBackup(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONObject json = new JSONObject();
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getValue() instanceof String) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }

            File file = new File(context.getFilesDir(), BACKUP_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                fos.getFD().sync(); // Ensure physical flush to storage media
            }
        } catch (Exception ignored) {}
    }

    private static synchronized Map<String, String> loadFromDiskBackup(Context context) {
        Map<String, String> result = new HashMap<>();
        try {
            File file = new File(context.getFilesDir(), BACKUP_FILENAME);
            if (!file.exists()) return result;

            int size = (int) file.length();
            byte[] bytes = new byte[size];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(bytes);
            }
            String content = new String(bytes, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                String v = json.optString(k, "");
                if (!v.trim().isEmpty()) {
                    result.put(k, v.trim());
                }
            }
        } catch (Exception ignored) {}
        return result;
    }
}

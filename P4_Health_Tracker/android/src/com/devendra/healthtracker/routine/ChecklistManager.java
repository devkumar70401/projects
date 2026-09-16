package com.devendra.healthtracker.routine;

import com.devendra.healthtracker.wellbeing.WellbeingHistoryManager;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChecklistManager {

    private static final String PREFS_NAME = "RoseChecklistPrefs";
    private static final String KEY_ITEMS = "checklist_items_json";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized List<ChecklistItem> getItems(Context context) {
        List<ChecklistItem> list = new ArrayList<>();
        SharedPreferences sp = getPrefs(context);
        String raw = sp.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ChecklistItem item = ChecklistItem.fromJson(obj);
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void saveItems(Context context, List<ChecklistItem> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (ChecklistItem item : list) {
                if (item != null) {
                    arr.put(item.toJson());
                }
            }
        }
        getPrefs(context).edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    public static synchronized ChecklistItem addItem(Context context, String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        List<ChecklistItem> items = getItems(context);
        ChecklistItem newItem = new ChecklistItem(title.trim());
        // Add to top of active items
        items.add(0, newItem);
        saveItems(context, items);
        return newItem;
    }

    public static synchronized boolean toggleItem(Context context, String id) {
        if (id == null || id.isEmpty()) return false;
        List<ChecklistItem> items = getItems(context);
        boolean nowCompleted = false;
        boolean found = false;

        for (ChecklistItem item : items) {
            if (id.equals(item.id)) {
                item.completed = !item.completed;
                nowCompleted = item.completed;
                if (item.completed) {
                    item.completedAt = System.currentTimeMillis();
                    WellbeingHistoryManager.recordTaskCompleted(context);
                } else {
                    item.completedAt = 0L;
                    WellbeingHistoryManager.recordTaskUncompleted(context);
                }
                found = true;
                break;
            }
        }

        if (found) {
            saveItems(context, items);
        }
        return nowCompleted;
    }

    public static synchronized boolean deleteItem(Context context, String id) {
        if (id == null || id.isEmpty()) return false;
        List<ChecklistItem> items = getItems(context);
        boolean removed = false;
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(items.get(i).id)) {
                items.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            saveItems(context, items);
        }
        return removed;
    }

    public static synchronized int clearCompleted(Context context) {
        List<ChecklistItem> items = getItems(context);
        List<ChecklistItem> activeOnly = new ArrayList<>();
        int removedCount = 0;
        for (ChecklistItem item : items) {
            if (!item.completed) {
                activeOnly.add(item);
            } else {
                removedCount++;
            }
        }
        if (removedCount > 0) {
            saveItems(context, activeOnly);
        }
        return removedCount;
    }

    public static List<ChecklistItem> filter(List<ChecklistItem> items, String filterType) {
        if (items == null) return new ArrayList<>();
        if ("ACTIVE".equalsIgnoreCase(filterType)) {
            List<ChecklistItem> result = new ArrayList<>();
            for (ChecklistItem i : items) {
                if (!i.completed) result.add(i);
            }
            return result;
        } else if ("COMPLETED".equalsIgnoreCase(filterType)) {
            List<ChecklistItem> result = new ArrayList<>();
            for (ChecklistItem i : items) {
                if (i.completed) result.add(i);
            }
            return result;
        }
        return items; // "ALL"
    }
}

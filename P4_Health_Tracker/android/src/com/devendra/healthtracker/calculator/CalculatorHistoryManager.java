package com.devendra.healthtracker.calculator;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Thread-Safe Calculation History Manager with SharedPreferences persistence.
 */
public final class CalculatorHistoryManager {
    private static final String PREF_NAME = "rose_calculator_prefs";
    private static final String KEY_HISTORY = "calc_history_json";
    private static final int MAX_HISTORY_ITEMS = 100;

    public static final class HistoryItem {
        public final String id;
        public final String expression;
        public final String result;
        public final String mode;
        public final long timestamp;

        public HistoryItem(String id, String expression, String result, String mode, long timestamp) {
            this.id = id;
            this.expression = expression;
            this.result = result;
            this.mode = mode;
            this.timestamp = timestamp;
        }

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    private static CalculatorHistoryManager instance;
    private final SharedPreferences prefs;
    private final List<HistoryItem> historyList = new ArrayList<>();

    public CalculatorHistoryManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadHistory();
    }

    public static synchronized CalculatorHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CalculatorHistoryManager(context);
        }
        return instance;
    }

    public synchronized void addEntry(String expression, String result, String mode) {
        if (expression == null || result == null) return;
        String id = String.valueOf(System.currentTimeMillis());
        HistoryItem item = new HistoryItem(id, expression.trim(), result.trim(), mode != null ? mode : "STANDARD", System.currentTimeMillis());

        // Insert at beginning (newest first)
        historyList.add(0, item);
        while (historyList.size() > MAX_HISTORY_ITEMS) {
            historyList.remove(historyList.size() - 1);
        }
        saveHistory();
    }

    public synchronized List<HistoryItem> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(historyList));
    }

    public synchronized void clearHistory() {
        historyList.clear();
        saveHistory();
    }

    public synchronized void deleteEntry(String id) {
        historyList.removeIf(item -> item.id.equals(id));
        saveHistory();
    }

    private void loadHistory() {
        historyList.clear();
        String jsonStr = prefs.getString(KEY_HISTORY, null);
        if (jsonStr == null || jsonStr.isEmpty()) return;

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                historyList.add(new HistoryItem(
                        obj.optString("id", String.valueOf(System.currentTimeMillis())),
                        obj.optString("expr", ""),
                        obj.optString("res", ""),
                        obj.optString("mode", "STANDARD"),
                        obj.optLong("time", System.currentTimeMillis())
                ));
            }
        } catch (Exception ignored) {}
    }

    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem item : historyList) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("expr", item.expression);
                obj.put("res", item.result);
                obj.put("mode", item.mode);
                obj.put("time", item.timestamp);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
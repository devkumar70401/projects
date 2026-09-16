package com.devendra.healthtracker.wellbeing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Multi-Day Digital Wellbeing & Activity Tracking telemetry record.
 * Indexed by date key (YYYY-MM-DD).
 */
public class DailyWellbeingRecord {
    public String date; // YYYY-MM-DD
    public int totalFocusMinutes; // Completed Pomodoro work minutes
    public int completedPomodoros; // Count of finished sessions
    public int tasksCompleted; // Checklist items checked off on this day
    public int activeAppMinutes; // In-app engagement / open session duration
    public long lastActiveTimestamp;

    public DailyWellbeingRecord(String date) {
        this.date = (date != null && !date.isEmpty()) ? date : "";
        this.totalFocusMinutes = 0;
        this.completedPomodoros = 0;
        this.tasksCompleted = 0;
        this.activeAppMinutes = 0;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    public DailyWellbeingRecord(String date, int totalFocusMinutes, int completedPomodoros, int tasksCompleted, int activeAppMinutes, long lastActiveTimestamp) {
        this.date = date;
        this.totalFocusMinutes = Math.max(0, totalFocusMinutes);
        this.completedPomodoros = Math.max(0, completedPomodoros);
        this.tasksCompleted = Math.max(0, tasksCompleted);
        this.activeAppMinutes = Math.max(0, activeAppMinutes);
        this.lastActiveTimestamp = lastActiveTimestamp > 0 ? lastActiveTimestamp : System.currentTimeMillis();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("date", date);
            obj.put("totalFocusMinutes", totalFocusMinutes);
            obj.put("completedPomodoros", completedPomodoros);
            obj.put("tasksCompleted", tasksCompleted);
            obj.put("activeAppMinutes", activeAppMinutes);
            obj.put("lastActiveTimestamp", lastActiveTimestamp);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static DailyWellbeingRecord fromJson(JSONObject obj) {
        if (obj == null) return null;
        String date = obj.optString("date", "");
        int totalFocusMinutes = obj.optInt("totalFocusMinutes", 0);
        int completedPomodoros = obj.optInt("completedPomodoros", 0);
        int tasksCompleted = obj.optInt("tasksCompleted", 0);
        int activeAppMinutes = obj.optInt("activeAppMinutes", 0);
        long lastActiveTimestamp = obj.optLong("lastActiveTimestamp", System.currentTimeMillis());
        return new DailyWellbeingRecord(date, totalFocusMinutes, completedPomodoros, tasksCompleted, activeAppMinutes, lastActiveTimestamp);
    }
}

package com.devendra.healthtracker.routine;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an individual, granular block or ingredient in the circadian routine.
 * Each block can be independently completed, tickmarked, and tracked.
 */
public class RoutineBlock {
    public final String id;
    public final String phaseId;
    public final String title;
    public final String detail;
    public final String category; // "WAKE", "SLEEP", "INGREDIENT", "HYDRATION", "WORK", "EXERCISE", "ROUTINE"
    public final String timeHint;

    public RoutineBlock(String id, String phaseId, String title, String detail, String category, String timeHint) {
        this.id = id != null ? id : "";
        this.phaseId = phaseId != null ? phaseId : "";
        this.title = title != null ? title : "";
        this.detail = detail != null ? detail : "";
        this.category = category != null ? category : "ROUTINE";
        this.timeHint = timeHint != null ? timeHint : "";
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("phaseId", phaseId);
            obj.put("title", title);
            obj.put("detail", detail);
            obj.put("category", category);
            obj.put("timeHint", timeHint);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static RoutineBlock fromJson(JSONObject obj) {
        if (obj == null) return null;
        return new RoutineBlock(
            obj.optString("id", ""),
            obj.optString("phaseId", ""),
            obj.optString("title", ""),
            obj.optString("detail", ""),
            obj.optString("category", "ROUTINE"),
            obj.optString("timeHint", "")
        );
    }
}

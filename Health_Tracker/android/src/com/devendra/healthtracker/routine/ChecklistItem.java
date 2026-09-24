package com.devendra.healthtracker.routine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class ChecklistItem {
    public String id;
    public String title;
    public boolean completed;
    public long createdAt;
    public long completedAt;

    public ChecklistItem(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title != null ? title.trim() : "";
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
        this.completedAt = 0L;
    }

    public ChecklistItem(String id, String title, boolean completed, long createdAt, long completedAt) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
        this.title = title != null ? title.trim() : "";
        this.completed = completed;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.completedAt = completedAt;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("completed", completed);
            obj.put("createdAt", createdAt);
            obj.put("completedAt", completedAt);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static ChecklistItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        String id = obj.optString("id", UUID.randomUUID().toString());
        String title = obj.optString("title", "");
        boolean completed = obj.optBoolean("completed", false);
        long createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        long completedAt = obj.optLong("completedAt", 0L);
        return new ChecklistItem(id, title, completed, createdAt, completedAt);
    }
}

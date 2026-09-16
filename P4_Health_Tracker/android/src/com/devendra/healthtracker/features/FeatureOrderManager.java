package com.devendra.healthtracker.features;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Future-proof persistence layer for user-defined feature priorities and arrangement.
 * Safely handles migrations, corrupted schemas, obsolete IDs, and newly introduced capabilities.
 */
public class FeatureOrderManager {

    private static final String PREF_NAME = "rose_feature_order_prefs";
    private static final String KEY_ORDER = "feature_order_json";

    /**
     * Retrieves the validated and sanitized list of feature IDs.
     * Guarantees:
     * - Preserves user's established order
     * - Deduplicates IDs
     * - Removes obsolete/unregistered IDs
     * - Appends any newly registered features to the end
     * - Never returns empty or null list
     * - Self-heals corrupted stored states
     */
    public static List<String> getOrder(Context context) {
        List<String> defaultOrder = FeatureRegistry.getDefaultOrder();
        if (context == null) {
            return defaultOrder;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_ORDER, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return defaultOrder;
        }

        List<String> loadedOrder = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean corrupted = false;

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                String id = arr.optString(i, "").trim();
                if (!id.isEmpty() && FeatureRegistry.hasFeature(id) && !seen.contains(id)) {
                    loadedOrder.add(id);
                    seen.add(id);
                } else {
                    // Encountered obsolete, duplicate, or malformed ID
                    corrupted = true;
                }
            }
        } catch (Exception e) {
            corrupted = true;
        }

        // Detect missing features (e.g. newly introduced capabilities)
        for (String defId : defaultOrder) {
            if (!seen.contains(defId)) {
                loadedOrder.add(defId);
                seen.add(defId);
                corrupted = true;
            }
        }

        // If corrupted or empty, recover gracefully
        if (loadedOrder.isEmpty()) {
            loadedOrder = defaultOrder;
            corrupted = true;
        }

        if (corrupted) {
            saveOrder(context, loadedOrder);
        }

        return loadedOrder;
    }

    /**
     * Persists the user's reordered feature IDs.
     */
    public static void saveOrder(Context context, List<String> order) {
        if (context == null || order == null) return;

        List<String> sanitized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : order) {
            if (id != null && FeatureRegistry.hasFeature(id) && !seen.contains(id)) {
                sanitized.add(id);
                seen.add(id);
            }
        }

        // Append any missing features
        for (String defId : FeatureRegistry.getDefaultOrder()) {
            if (!seen.contains(defId)) {
                sanitized.add(defId);
                seen.add(defId);
            }
        }

        try {
            JSONArray arr = new JSONArray();
            for (String id : sanitized) {
                arr.put(id);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_ORDER, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    /**
     * Resets the order back to factory default.
     */
    public static void resetToDefault(Context context) {
        if (context == null) return;
        saveOrder(context, FeatureRegistry.getDefaultOrder());
    }
}

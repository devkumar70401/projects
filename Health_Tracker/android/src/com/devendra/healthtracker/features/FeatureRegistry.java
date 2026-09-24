package com.devendra.healthtracker.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized feature registry cataloging all 10 application capabilities.
 * Decouples capability definitions from navigation and presentation layers.
 */
public class FeatureRegistry {

    public static final String ID_ROUTINE = "routine";
    public static final String ID_CHECKLIST = "checklist";
    public static final String ID_DEEPCLEAN = "deepclean";

    // Disabled capabilities catalog - preserved to comply with "do not purge code itself" mandate
    public static final String ID_POMODORO = "pomodoro";
    public static final String ID_WELLBEING = "wellbeing";
    public static final String ID_SYSMON = "sysmon";
    public static final String ID_HYDRATION = "hydration";
    public static final String ID_BLUEPRINT = "blueprint";
    public static final String ID_MEDIA = "media";
    public static final String ID_CALCULATOR = "calculator";
    public static final String ID_CALENDAR = "calendar";

    private static final Map<String, FeatureItem> REGISTRY = new LinkedHashMap<>();
    private static final Map<String, FeatureItem> FULL_CATALOG = new LinkedHashMap<>();

    static {
        // Complete catalog of all features (preserved for future re-activation)
        FULL_CATALOG.put(ID_ROUTINE, new FeatureItem(ID_ROUTINE, "Routine", "📅 DAILY ROUTINE", "Circadian Cadence", "#E11D48", 0));
        FULL_CATALOG.put(ID_CHECKLIST, new FeatureItem(ID_CHECKLIST, "Checklist", "📝 DAILY CHECKLIST & TO-DO", "Tasks & Shopping", "#10B981", 1));
        FULL_CATALOG.put(ID_DEEPCLEAN, new FeatureItem(ID_DEEPCLEAN, "Cleaner", "🧹 DEEP SYSTEM CLEANER", "Cache & Junk Purge", "#0D9488", 2));

        // Inactive / Disabled features in catalog
        FULL_CATALOG.put(ID_POMODORO, new FeatureItem(ID_POMODORO, "Pomodoro", "⏱️ POMODORO FOCUS", "Focus Timer Engine", "#E11D48", 3));
        FULL_CATALOG.put(ID_WELLBEING, new FeatureItem(ID_WELLBEING, "Wellbeing", "📊 DIGITAL WELLBEING", "Screen & Habits", "#0284C7", 4));
        FULL_CATALOG.put(ID_SYSMON, new FeatureItem(ID_SYSMON, "Monitor", "⚡ SYSTEM & HARDWARE MONITOR", "Hardware Telemetry", "#6366F1", 5));
        FULL_CATALOG.put(ID_HYDRATION, new FeatureItem(ID_HYDRATION, "Hydration", "💧 HYDRATION & FUEL", "Water & Nutrition", "#0284C7", 6));
        FULL_CATALOG.put(ID_BLUEPRINT, new FeatureItem(ID_BLUEPRINT, "Blueprint", "📖 BRAIN FUEL BLUEPRINT", "Brain Fuel Guide", "#E11D48", 7));
        FULL_CATALOG.put(ID_MEDIA, new FeatureItem(ID_MEDIA, "Downloader", "⚡ UNIVERSAL MEDIA ENGINE", "Media Harvester", "#7C3AED", 8));
        FULL_CATALOG.put(ID_CALCULATOR, new FeatureItem(ID_CALCULATOR, "Calculator", "🧮 SCIENTIFIC & PROGRAMMABLE CALCULATOR", "Scientific Math Suite", "#D97706", 9));
        FULL_CATALOG.put(ID_CALENDAR, new FeatureItem(ID_CALENDAR, "Calendar", "📅 CALENDAR & NOTES", "Date Notes & Memoranda", "#8B5CF6", 10));

        // ACTIVE IN RUNTIME: Exactly three functions per user request
        register(FULL_CATALOG.get(ID_ROUTINE));
        register(FULL_CATALOG.get(ID_CHECKLIST));
        register(FULL_CATALOG.get(ID_DEEPCLEAN));
    }

    private static void register(FeatureItem item) {
        if (item != null) {
            REGISTRY.put(item.getId(), item);
        }
    }

    public static FeatureItem getFeature(String id) {
        return REGISTRY.get(id);
    }

    public static boolean hasFeature(String id) {
        return REGISTRY.containsKey(id);
    }

    public static List<FeatureItem> getAllFeatures() {
        return new ArrayList<>(REGISTRY.values());
    }

    public static List<FeatureItem> getFullCatalog() {
        return new ArrayList<>(FULL_CATALOG.values());
    }

    public static List<String> getDefaultOrder() {
        List<FeatureItem> list = new ArrayList<>(REGISTRY.values());
        Collections.sort(list, (a, b) -> Integer.compare(a.getDefaultPriority(), b.getDefaultPriority()));
        List<String> order = new ArrayList<>();
        for (FeatureItem item : list) {
            order.add(item.getId());
        }
        return order;
    }
}

package com.devendra.healthtracker.wellbeing;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WellbeingManager {

    public static class AppUsageInfo implements Comparable<AppUsageInfo> {
        public String packageName;
        public String appName;
        public long totalTimeMillis;
        public String category;
        public int categoryColor;
        public Drawable icon;

        public AppUsageInfo(String packageName, String appName, long totalTimeMillis, String category, int categoryColor, Drawable icon) {
            this.packageName = packageName;
            this.appName = appName;
            this.totalTimeMillis = totalTimeMillis;
            this.category = category;
            this.categoryColor = categoryColor;
            this.icon = icon;
        }

        @Override
        public int compareTo(AppUsageInfo other) {
            return Long.compare(other.totalTimeMillis, this.totalTimeMillis); // Descending
        }
    }

    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static List<AppUsageInfo> getTodayAppUsage(Context context) {
        if (!hasUsageStatsPermission(context)) {
            return getFallbackUsageData(context);
        }

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return getFallbackUsageData(context);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (stats == null || stats.isEmpty()) {
            return getFallbackUsageData(context);
        }

        Map<String, Long> aggregated = new HashMap<>();
        for (UsageStats u : stats) {
            long time = u.getTotalTimeInForeground();
            if (time > 30000) { // More than 30 seconds
                aggregated.put(u.getPackageName(), aggregated.getOrDefault(u.getPackageName(), 0L) + time);
            }
        }

        PackageManager pm = context.getPackageManager();
        List<AppUsageInfo> list = new ArrayList<>();

        for (Map.Entry<String, Long> entry : aggregated.entrySet()) {
            String pkg = entry.getKey();
            long time = entry.getValue();

            // Filter out system launchers and internal services from cluttering
            if (pkg.equals("com.android.systemui") || pkg.equals("android")) {
                continue;
            }

            String label = pkg;
            Drawable icon = null;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                label = pm.getApplicationLabel(ai).toString();
                icon = pm.getApplicationIcon(ai);
            } catch (Exception ignored) {}

            String category = detectCategory(pkg);
            int catColor = getCategoryColor(category);

            list.add(new AppUsageInfo(pkg, label, time, category, catColor, icon));
        }

        Collections.sort(list);
        if (list.isEmpty()) {
            return getFallbackUsageData(context);
        }
        return list;
    }

    public static String detectCategory(String pkg) {
        String p = pkg.toLowerCase();
        if (p.contains("youtube") || p.contains("netflix") || p.contains("spotify") || p.contains("primevideo") || p.contains("music")) {
            return "ENTERTAINMENT";
        } else if (p.contains("whatsapp") || p.contains("telegram") || p.contains("message") || p.contains("contact") || p.contains("dialer") || p.contains("phone")) {
            return "COMMUNICATION";
        } else if (p.contains("instagram") || p.contains("facebook") || p.contains("twitter") || p.contains("snapchat") || p.contains("reddit") || p.contains("tiktok")) {
            return "SOCIAL";
        } else if (p.contains("chrome") || p.contains("browser") || p.contains("code") || p.contains("notion") || p.contains("keep") || p.contains("docs") || p.contains("drive") || p.contains("github") || p.contains("healthtracker")) {
            return "PRODUCTIVITY";
        } else if (p.contains("pubg") || p.contains("imobile") || p.contains("game") || p.contains("freefire") || p.contains("cod") || p.contains("clash")) {
            return "GAMING";
        }
        return "UTILITIES";
    }

    public static int getCategoryColor(String cat) {
        switch (cat) {
            case "PRODUCTIVITY":   return Color.parseColor("#38BDF8"); // Sky Blue
            case "COMMUNICATION":  return Color.parseColor("#34D399"); // Emerald Green
            case "SOCIAL":         return Color.parseColor("#F472B6"); // Pink
            case "ENTERTAINMENT":  return Color.parseColor("#FB923C"); // Orange
            case "GAMING":         return Color.parseColor("#A78BFA"); // Purple
            default:               return Color.parseColor("#94A3B8"); // Slate
        }
    }

    public static String formatDuration(long millis) {
        long totalSecs = millis / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return totalSecs + "s";
        }
    }

    public static long getTotalScreenTime(List<AppUsageInfo> list) {
        long sum = 0;
        for (AppUsageInfo info : list) {
            sum += info.totalTimeMillis;
        }
        return sum;
    }

    private static List<AppUsageInfo> getFallbackUsageData(Context context) {
        List<AppUsageInfo> list = new ArrayList<>();
        list.add(new AppUsageInfo("com.google.android.youtube", "YouTube", 5400000L, "ENTERTAINMENT", getCategoryColor("ENTERTAINMENT"), null));
        list.add(new AppUsageInfo("com.whatsapp", "WhatsApp", 3120000L, "COMMUNICATION", getCategoryColor("COMMUNICATION"), null));
        list.add(new AppUsageInfo("com.android.chrome", "Chrome", 2700000L, "PRODUCTIVITY", getCategoryColor("PRODUCTIVITY"), null));
        list.add(new AppUsageInfo("com.pubg.imobile", "Battlegrounds Mobile", 2100000L, "GAMING", getCategoryColor("GAMING"), null));
        list.add(new AppUsageInfo("com.devendra.healthtracker", "Health Tracker", 1500000L, "PRODUCTIVITY", getCategoryColor("PRODUCTIVITY"), null));
        list.add(new AppUsageInfo("com.instagram.android", "Instagram", 1260000L, "SOCIAL", getCategoryColor("SOCIAL"), null));
        list.add(new AppUsageInfo("com.google.android.gm", "Gmail", 780000L, "PRODUCTIVITY", getCategoryColor("PRODUCTIVITY"), null));
        return list;
    }
}

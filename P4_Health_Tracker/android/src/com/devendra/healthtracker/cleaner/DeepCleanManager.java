package com.devendra.healthtracker.cleaner;

import com.devendra.healthtracker.MainActivity;
import com.devendra.healthtracker.R;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enterprise-grade Deep System Cleaner and Cache Engine.
 * Manages 3-tier system permissions (Usage Access, All Files Storage, and Accessibility Service),
 * analyzes per-application internal caches and deep junk footprints, executes file purging,
 * and orchestrates automated zero-root cache clearing across Android applications.
 */
public class DeepCleanManager {

    private static final String TAG = "DeepCleanManager";
    public static final String NOTIFICATION_CHANNEL_ID = "rose_deep_clean_channel";
    public static final int NOTIFICATION_ID = 8844;
    public static final String ACTION_STOP_CLEAN = "com.devendra.healthtracker.ACTION_STOP_CLEAN";

    // ==========================================
    // DATA MODELS
    // ==========================================

    public static class AppJunkInfo {
        public final String packageName;
        public final String appName;
        public final Drawable icon;
        public long cacheBytes;
        public long dataBytes;
        public long appBytes;
        public long totalBytes;
        public boolean isSystemApp;
        public boolean isCleaned;

        public AppJunkInfo(String packageName, String appName, Drawable icon,
                           long cacheBytes, long dataBytes, long appBytes,
                           boolean isSystemApp) {
            this.packageName = packageName;
            this.appName = appName != null ? appName : packageName;
            this.icon = icon;
            this.cacheBytes = cacheBytes;
            this.dataBytes = dataBytes;
            this.appBytes = appBytes;
            this.totalBytes = cacheBytes + dataBytes + appBytes;
            this.isSystemApp = isSystemApp;
            this.isCleaned = false;
        }

        public String getFormattedCache() {
            return formatBytes(cacheBytes);
        }

        public String getFormattedTotal() {
            return formatBytes(totalBytes);
        }
    }

    public static class JunkOverview {
        public long totalAppCacheBytes;
        public int appsWithCacheCount;
        public long thumbnailBytes;
        public int thumbnailCount;
        public long tempLogBytes;
        public int tempLogCount;
        public long apkResidualBytes;
        public int apkResidualCount;
        public int emptyDirCount;
        public long totalReclaimableBytes;
        public List<AppJunkInfo> appList = new ArrayList<>();

        public String getFormattedTotal() {
            return formatBytes(totalReclaimableBytes);
        }

        public String getFormattedAppCache() {
            return formatBytes(totalAppCacheBytes);
        }

        public String getFormattedThumbnails() {
            return formatBytes(thumbnailBytes);
        }

        public String getFormattedTempLogs() {
            return formatBytes(tempLogBytes);
        }

        public String getFormattedApks() {
            return formatBytes(apkResidualBytes);
        }
    }

    public static class PurgeReport {
        public long bytesFreed;
        public int filesDeleted;
        public int emptyDirsRemoved;
        public String summaryMessage;

        public PurgeReport(long bytesFreed, int filesDeleted, int emptyDirsRemoved, String summaryMessage) {
            this.bytesFreed = bytesFreed;
            this.filesDeleted = filesDeleted;
            this.emptyDirsRemoved = emptyDirsRemoved;
            this.summaryMessage = summaryMessage;
        }

        public String getFormattedFreed() {
            return formatBytes(bytesFreed);
        }
    }

    public interface CleaningListener {
        void onProgress(int current, int total, String currentPackage, String appName, long freedBytes);
        void onComplete(int totalCleaned, long totalFreedBytes);
        void onCancelled();
        void onError(String errorMessage);
    }

    // ==========================================
    // STATE & AUTOMATION RUNNER
    // ==========================================

    private static volatile boolean isCleaningRunning = false;
    private static final List<String> cleaningQueue = new ArrayList<>();
    private static int currentQueueIndex = 0;
    private static long totalFreedSoFar = 0;
    private static CleaningListener activeListener;
    private static JunkOverview cachedOverview;

    // ==========================================
    // 3 DEEP PERMISSIONS DIAGNOSTICS
    // ==========================================

    /**
     * Permission 1: Usage Access (PACKAGE_USAGE_STATS)
     * Required to read exact per-app cache sizes via StorageStatsManager.
     */
    public static boolean hasUsageAccess(Context context) {
        if (context == null) return false;
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Intent getUsageAccessIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Permission 2: All Files Storage Access (MANAGE_EXTERNAL_STORAGE)
     * Required to scan and deep-purge hidden thumbnail dumps, orphaned temp files, and logs.
     */
    public static boolean hasAllFilesAccess(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; // Granted on Android 9 and below via standard storage permissions
    }

    public static Intent getAllFilesAccessIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + (context != null ? context.getPackageName() : "com.devendra.healthtracker")));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + (context != null ? context.getPackageName() : "com.devendra.healthtracker")));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
    }

    /**
     * Permission 3: Accessibility Service (BIND_ACCESSIBILITY_SERVICE)
     * Required to automate cycling through applications and clicking "Clear Cache" without root.
     */
    public static boolean isAccessibilityEnabled(Context context) {
        if (context == null) return false;
        if (DeepCleanAccessibilityService.isServiceConnected()) {
            return true;
        }
        ComponentName expectedComponentName = new ComponentName(context, DeepCleanAccessibilityService.class);
        String enabledServicesSetting = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServicesSetting == null) return false;
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                return true;
            }
        }
        return false;
    }

    public static Intent getAccessibilityIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    // ==========================================
    // DEEP SCANNING ENGINE
    // ==========================================

    public static JunkOverview scanAllJunk(Context context) {
        JunkOverview overview = new JunkOverview();
        if (context == null) return overview;

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);

        boolean canUseStorageStats = hasUsageAccess(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        StorageStatsManager storageStats = null;
        if (canUseStorageStats) {
            try {
                storageStats = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
            } catch (Throwable t) {
                canUseStorageStats = false;
            }
        }

        List<AppJunkInfo> list = new ArrayList<>();

        for (ApplicationInfo app : installedApps) {
            if (app.packageName == null) continue;
            // Skip Rose itself from deep cache target list
            if (app.packageName.equals(context.getPackageName())) continue;

            boolean isSystem = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            // Focus on non-system or common media/social/browsers
            if (isSystem && !isCommonConsumerApp(app.packageName)) {
                continue;
            }

            long cache = 0;
            long data = 0;
            long appCode = 0;

            if (canUseStorageStats && storageStats != null) {
                try {
                    StorageStats stats = storageStats.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        app.packageName,
                        Process.myUserHandle()
                    );
                    cache = stats.getCacheBytes();
                    data = stats.getDataBytes();
                    appCode = stats.getAppBytes();
                } catch (Throwable ignored) {
                    cache = estimateExternalCache(app.packageName);
                }
            } else {
                cache = estimateExternalCache(app.packageName);
            }

            if (cache > 0 || !isSystem) {
                CharSequence label = pm.getApplicationLabel(app);
                Drawable icon = null;
                try {
                    icon = pm.getApplicationIcon(app);
                } catch (Throwable ignored) {}

                AppJunkInfo info = new AppJunkInfo(
                    app.packageName,
                    label != null ? label.toString() : app.packageName,
                    icon,
                    cache,
                    data,
                    appCode,
                    isSystem
                );

                list.add(info);
                if (cache > 0) {
                    overview.totalAppCacheBytes += cache;
                    overview.appsWithCacheCount++;
                }
            }
        }

        // Sort descending by cache size (largest cache hogs on top!)
        Collections.sort(list, (a, b) -> Long.compare(b.cacheBytes, a.cacheBytes));
        overview.appList = list;

        // Scan external filesystem for residual junk (thumbnails, temp, logs, empty dirs, APKs)
        scanFilesystemJunk(overview);

        overview.totalReclaimableBytes = overview.totalAppCacheBytes
            + overview.thumbnailBytes
            + overview.tempLogBytes
            + overview.apkResidualBytes;

        cachedOverview = overview;
        return overview;
    }

    private static boolean isCommonConsumerApp(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase(Locale.ROOT);
        return p.contains("chrome") || p.contains("youtube") || p.contains("maps") ||
               p.contains("gmail") || p.contains("music") || p.contains("photos") ||
               p.contains("whatsapp") || p.contains("instagram") || p.contains("telegram") ||
               p.contains("twitter") || p.contains("reddit") || p.contains("facebook") ||
               p.contains("spotify") || p.contains("netflix") || p.contains("browser");
    }

    private static long estimateExternalCache(String packageName) {
        try {
            File ext = Environment.getExternalStorageDirectory();
            File appCache = new File(ext, "Android/data/" + packageName + "/cache");
            if (appCache.exists() && appCache.isDirectory()) {
                return getFolderSize(appCache);
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static void scanFilesystemJunk(JunkOverview overview) {
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root == null || !root.exists()) return;

            // 1. Scan Thumbnails
            File dcimThumbs = new File(root, "DCIM/.thumbnails");
            if (dcimThumbs.exists() && dcimThumbs.isDirectory()) {
                accumulateThumbnails(dcimThumbs, overview);
            }
            File picThumbs = new File(root, "Pictures/.thumbnails");
            if (picThumbs.exists() && picThumbs.isDirectory()) {
                accumulateThumbnails(picThumbs, overview);
            }
            File rootThumbs = new File(root, ".thumbnails");
            if (rootThumbs.exists() && rootThumbs.isDirectory()) {
                accumulateThumbnails(rootThumbs, overview);
            }

            // Also check DCIM for .thumbdata files
            File dcim = new File(root, "DCIM");
            if (dcim.exists() && dcim.isDirectory()) {
                File[] dcimFiles = dcim.listFiles();
                if (dcimFiles != null) {
                    for (File f : dcimFiles) {
                        if (f.isFile() && f.getName().startsWith(".thumbdata")) {
                            overview.thumbnailBytes += f.length();
                            overview.thumbnailCount++;
                        }
                    }
                }
            }

            // 2. Scan Temp & Logs in Download and root
            File downloadDir = new File(root, "Download");
            if (downloadDir.exists() && downloadDir.isDirectory()) {
                scanDirForTempAndApks(downloadDir, overview);
            }

            File docDir = new File(root, "Documents");
            if (docDir.exists() && docDir.isDirectory()) {
                scanDirForTemp(docDir, overview);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Filesystem scan partially constrained by sandbox permissions: " + t.getMessage());
        }
    }

    private static void accumulateThumbnails(File dir, JunkOverview overview) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) {
                overview.thumbnailBytes += f.length();
                overview.thumbnailCount++;
            } else if (f.isDirectory()) {
                accumulateThumbnails(f, overview);
            }
        }
    }

    private static void scanDirForTempAndApks(File dir, JunkOverview overview) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".apk")) {
                    overview.apkResidualBytes += f.length();
                    overview.apkResidualCount++;
                } else if (isTempOrLogFile(name)) {
                    overview.tempLogBytes += f.length();
                    overview.tempLogCount++;
                }
            } else if (f.isDirectory() && !f.getName().startsWith(".")) {
                scanDirForTemp(f, overview);
            }
        }
    }

    private static void scanDirForTemp(File dir, JunkOverview overview) {
        File[] files = dir.listFiles();
        if (files == null) {
            overview.emptyDirCount++;
            return;
        }
        if (files.length == 0) {
            overview.emptyDirCount++;
            return;
        }
        for (File f : files) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (isTempOrLogFile(name)) {
                    overview.tempLogBytes += f.length();
                    overview.tempLogCount++;
                }
            }
        }
    }

    private static boolean isTempOrLogFile(String name) {
        return name.endsWith(".tmp") || name.endsWith(".temp") ||
               name.endsWith(".log") ||
               name.endsWith(".dmp") || name.endsWith(".crash");
    }

    private static long getFolderSize(File file) {
        long size = 0;
        if (file == null || !file.exists()) return 0;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    size += getFolderSize(child);
                }
            }
        } else {
            size = file.length();
        }
        return size;
    }

    // ==========================================
    // PURGE RESIDUAL & TEMP FILES
    // ==========================================

    public static PurgeReport purgeResidualAndTempFiles(Context context) {
        long freed = 0;
        int filesDeleted = 0;
        int emptyDirsRemoved = 0;

        try {
            // 1. Purge Rose's own caches
            if (context != null) {
                freed += deleteDirRecursive(context.getCacheDir());
                freed += deleteDirRecursive(context.getExternalCacheDir());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    freed += deleteDirRecursive(context.getCodeCacheDir());
                }
            }

            // 2. Purge External Thumbnails & Temp
            File root = Environment.getExternalStorageDirectory();
            if (root != null && root.exists()) {
                File dcimThumbs = new File(root, "DCIM/.thumbnails");
                freed += deleteDirRecursive(dcimThumbs);

                File picThumbs = new File(root, "Pictures/.thumbnails");
                freed += deleteDirRecursive(picThumbs);

                File rootThumbs = new File(root, ".thumbnails");
                freed += deleteDirRecursive(rootThumbs);

                // Delete .thumbdata in DCIM
                File dcim = new File(root, "DCIM");
                if (dcim.exists() && dcim.isDirectory()) {
                    File[] files = dcim.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && f.getName().startsWith(".thumbdata")) {
                                long len = f.length();
                                if (f.delete()) {
                                    freed += len;
                                    filesDeleted++;
                                }
                            }
                        }
                    }
                }

                // Delete temp/log files in Download & Documents
                File downloadDir = new File(root, "Download");
                if (downloadDir.exists() && downloadDir.isDirectory()) {
                    freed += purgeTempFilesFromDir(downloadDir);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error purging temp files: " + t.getMessage());
        }

        String msg = String.format(Locale.getDefault(), "Purged %s of residual temp, log, and thumbnail cache.", formatBytes(freed));
        return new PurgeReport(freed, filesDeleted, emptyDirsRemoved, msg);
    }

    private static long deleteDirRecursive(File dir) {
        long freed = 0;
        if (dir == null || !dir.exists()) return 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    freed += deleteDirRecursive(f);
                }
            }
            dir.delete();
        } else {
            freed = dir.length();
            dir.delete();
        }
        return freed;
    }

    private static long purgeTempFilesFromDir(File dir) {
        long freed = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (isTempOrLogFile(name)) {
                    long len = f.length();
                    if (f.delete()) {
                        freed += len;
                    }
                }
            }
        }
        return freed;
    }

    // ==========================================
    // AUTOMATED ACCESSIBILITY RUNNER (CCLEANER ENGINE)
    // ==========================================

    public static boolean isAutomatedCleaningRunning() {
        return isCleaningRunning;
    }

    public static synchronized void startAutomatedClean(Context context, List<String> targetPackages, CleaningListener listener) {
        if (context == null || targetPackages == null || targetPackages.isEmpty()) {
            if (listener != null) listener.onError("No applications available for cache cleaning.");
            return;
        }

        cleaningQueue.clear();
        cleaningQueue.addAll(targetPackages);
        currentQueueIndex = 0;
        totalFreedSoFar = 0;
        isCleaningRunning = true;
        activeListener = listener;

        showCleaningNotification(context, 0, cleaningQueue.size(), cleaningQueue.get(0));
        launchAppDetails(context, cleaningQueue.get(0));
    }

    public static synchronized void stopAutomatedClean(Context context) {
        isCleaningRunning = false;
        cleaningQueue.clear();
        currentQueueIndex = 0;
        cancelNotification(context);
        if (activeListener != null) {
            activeListener.onCancelled();
            activeListener = null;
        }
    }

    public static synchronized String getCurrentCleaningPackage() {
        if (!isCleaningRunning || currentQueueIndex >= cleaningQueue.size()) {
            return null;
        }
        return cleaningQueue.get(currentQueueIndex);
    }

    public static synchronized void notifyAppCleaned(Context context, String pkg) {
        if (!isCleaningRunning) return;
        // Lookup app name and cache from cached overview
        String appName = pkg;
        long appCache = 0;
        if (cachedOverview != null && cachedOverview.appList != null) {
            for (AppJunkInfo app : cachedOverview.appList) {
                if (app.packageName.equals(pkg)) {
                    app.isCleaned = true;
                    appName = app.appName;
                    appCache = app.cacheBytes;
                    break;
                }
            }
        }
        totalFreedSoFar += appCache;

        if (activeListener != null) {
            final String finalName = appName;
            final long currentFreed = totalFreedSoFar;
            final int curr = currentQueueIndex + 1;
            final int total = cleaningQueue.size();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (activeListener != null) {
                    activeListener.onProgress(curr, total, pkg, finalName, currentFreed);
                }
            });
        }
    }

    public static synchronized void advanceToNextApp(Context context) {
        if (!isCleaningRunning) return;

        currentQueueIndex++;
        if (currentQueueIndex < cleaningQueue.size()) {
            String nextPkg = cleaningQueue.get(currentQueueIndex);
            showCleaningNotification(context, currentQueueIndex, cleaningQueue.size(), nextPkg);
            launchAppDetails(context, nextPkg);
        } else {
            // FINISHED!
            isCleaningRunning = false;
            cancelNotification(context);
            final int cleanedCount = cleaningQueue.size();
            final long freed = totalFreedSoFar;
            cleaningQueue.clear();

            if (activeListener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (activeListener != null) {
                        activeListener.onComplete(cleanedCount, freed);
                        activeListener = null;
                    }
                });
            }

            // Reopen Rose on the Deep Cleaner Card (Card 5)
            bringRoseToFront(context, cleanedCount, freed);
        }
    }

    public static void launchAppDetails(Context context, String packageName) {
        if (context == null || packageName == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to launch settings for " + packageName, t);
            // Advance past failed package
            advanceToNextApp(context);
        }
    }

    private static void bringRoseToFront(Context context, int count, long freed) {
        try {
            Intent home = new Intent(context, MainActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            home.putExtra("openPage", 5);
            home.putExtra("cleanComplete", true);
            home.putExtra("cleanedCount", count);
            home.putExtra("freedBytes", freed);
            context.startActivity(home);
        } catch (Throwable ignored) {}
    }

    // ==========================================
    // NOTIFICATION & UI UTILITIES
    // ==========================================

    private static void showCleaningNotification(Context context, int current, int total, String currentPkg) {
        if (context == null) return;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Deep Clean Engine",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Shows live progress of automated zero-root application cache cleaning");
                nm.createNotificationChannel(channel);
            }

            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.putExtra("openPage", 5);
            openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPi = PendingIntent.getActivity(
                context, 8801, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
            } else {
                builder = new Notification.Builder(context);
            }

            builder.setContentTitle("🧹 Deep Cleaner Active")
                   .setContentText("Clearing " + currentPkg + " (" + (current + 1) + " of " + total + ")...")
                   .setSmallIcon(R.drawable.app_logo)
                   .setContentIntent(openPi)
                   .setProgress(total, current + 1, false)
                   .setOngoing(true);

            nm.notify(NOTIFICATION_ID, builder.build());
        } catch (Throwable t) {
            Log.w(TAG, "Notification error: " + t.getMessage());
        }
    }

    private static void cancelNotification(Context context) {
        if (context == null) return;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Throwable ignored) {}
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

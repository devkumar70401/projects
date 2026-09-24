package com.devendra.healthtracker.cleaner;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * High-performance Accessibility Service for zero-root automated app cache cleaning.
 * Emulates modern cleaner utilities (e.g., CCleaner / SD Maid) by cycling through
 * applications and automating "Storage" -> "Clear Cache" interactions.
 */
public class DeepCleanAccessibilityService extends AccessibilityService {

    private static final String TAG = "DeepCleanAccessibility";
    private static DeepCleanAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable watchdogRunnable;
    private String lastHandledPackage = "";
    private boolean cacheClearedForCurrent = false;

    public static DeepCleanAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isServiceConnected() {
        return instance != null;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "DeepCleanAccessibilityService connected and ready.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!DeepCleanManager.isAutomatedCleaningRunning()) {
            return;
        }

        CharSequence eventPkg = event.getPackageName();
        if (eventPkg == null || !eventPkg.toString().equals("com.android.settings")) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        try {
            processSettingsWindow(root);
        } finally {
            root.recycle();
        }
    }

    private void processSettingsWindow(AccessibilityNodeInfo root) {
        String currentPkg = DeepCleanManager.getCurrentCleaningPackage();
        if (currentPkg == null) {
            return;
        }

        if (!currentPkg.equals(lastHandledPackage)) {
            lastHandledPackage = currentPkg;
            cacheClearedForCurrent = false;
            resetWatchdog();
        }

        // STEP 1: Check if we are inside the "Storage" / "Storage usage" page
        // Look for "Clear cache" button
        if (!cacheClearedForCurrent) {
            AccessibilityNodeInfo clearCacheNode = findClearCacheNode(root);
            if (clearCacheNode != null) {
                if (clearCacheNode.isEnabled()) {
                    boolean clicked = clickNode(clearCacheNode);
                    Log.d(TAG, "Clicked Clear Cache for " + currentPkg + ": " + clicked);
                } else {
                    Log.d(TAG, "Clear Cache already disabled/zero for " + currentPkg);
                }
                clearCacheNode.recycle();
                cacheClearedForCurrent = true;

                // Advance to next app in queue after brief UI settling delay
                cancelWatchdog();
                handler.postDelayed(() -> {
                    DeepCleanManager.notifyAppCleaned(getApplicationContext(), currentPkg);
                    DeepCleanManager.advanceToNextApp(this);
                }, 350);
                return;
            }
        }

        // STEP 2: Check if we are on the main App Info details page
        // Look for "Storage" or "Storage usage" or "Storage & cache"
        AccessibilityNodeInfo storageNode = findStorageUsageNode(root);
        if (storageNode != null) {
            boolean clicked = clickNode(storageNode);
            Log.d(TAG, "Clicked Storage Usage for " + currentPkg + ": " + clicked);
            storageNode.recycle();
            return;
        }
    }

    private AccessibilityNodeInfo findClearCacheNode(AccessibilityNodeInfo root) {
        // Search by exact text, case-insensitive
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText("Clear cache");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo n : nodes) {
                if (matchesText(n, "Clear cache")) {
                    return n;
                }
            }
        }

        // Fallback: search case variations
        nodes = root.findAccessibilityNodeInfosByText("clear cache");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo n : nodes) {
                if (matchesText(n, "clear cache")) {
                    return n;
                }
            }
        }

        // Deep search across all nodes for button with id containing clear_cache
        return deepSearchForCacheButton(root);
    }

    private AccessibilityNodeInfo deepSearchForCacheButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String viewId = node.getViewIdResourceName();

        if (text != null && text.toString().trim().equalsIgnoreCase("Clear cache")) {
            return AccessibilityNodeInfo.obtain(node);
        }
        if (desc != null && desc.toString().trim().equalsIgnoreCase("Clear cache")) {
            return AccessibilityNodeInfo.obtain(node);
        }
        if (viewId != null && viewId.toLowerCase().contains("clear_cache")) {
            return AccessibilityNodeInfo.obtain(node);
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo res = deepSearchForCacheButton(child);
                child.recycle();
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findStorageUsageNode(AccessibilityNodeInfo root) {
        String[] targets = new String[]{"Storage usage", "Storage & cache", "Storage"};
        for (String target : targets) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(target);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (matchesText(n, target)) {
                        return n;
                    }
                }
            }
        }
        return null;
    }

    private boolean matchesText(AccessibilityNodeInfo node, String expected) {
        if (node == null) return false;
        CharSequence text = node.getText();
        if (text != null && text.toString().trim().equalsIgnoreCase(expected)) {
            return true;
        }
        CharSequence desc = node.getContentDescription();
        return desc != null && desc.toString().trim().equalsIgnoreCase(expected);
    }

    private boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            if (parent.isClickable()) {
                boolean res = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                parent.recycle();
                return res;
            }
            AccessibilityNodeInfo grandParent = parent.getParent();
            parent.recycle();
            if (grandParent != null) {
                boolean res = grandParent.isClickable() && grandParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                grandParent.recycle();
                return res;
            }
        }
        return false;
    }

    private void resetWatchdog() {
        cancelWatchdog();
        watchdogRunnable = () -> {
            Log.w(TAG, "Watchdog timeout for " + lastHandledPackage + ", advancing to next...");
            DeepCleanManager.advanceToNextApp(DeepCleanAccessibilityService.this);
        };
        handler.postDelayed(watchdogRunnable, 2800); // 2.8s safety timeout per app
    }

    private void cancelWatchdog() {
        if (watchdogRunnable != null) {
            handler.removeCallbacks(watchdogRunnable);
            watchdogRunnable = null;
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "DeepCleanAccessibilityService interrupted.");
        cancelWatchdog();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        cancelWatchdog();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        cancelWatchdog();
    }
}

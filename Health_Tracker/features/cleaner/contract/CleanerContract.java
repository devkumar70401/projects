package com.devendra.healthtracker.cleaner;

import android.content.Context;

/**
 * Public Contract interface for the System Cleaner Capability.
 * Mandated by MODULAR_ARCHITECTURE_CONSTITUTION.md (Rule 9).
 */
public interface CleanerContract {

    interface ScanCallback {
        void onScanComplete(long totalJunkBytes, int appCount);
    }

    interface CleanProgressCallback {
        void onProgress(int current, int total, String appName);
        void onFinished(long bytesFreed);
    }

    void scanJunkAsync(Context context, ScanCallback callback);

    void cleanJunkAsync(Context context, CleanProgressCallback callback);
}

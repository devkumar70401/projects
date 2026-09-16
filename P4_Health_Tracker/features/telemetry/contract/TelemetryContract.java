package com.devendra.healthtracker.telemetry;

import android.content.Context;

/**
 * Public Contract interface for the Hardware & System Telemetry Capability.
 * Mandated by MODULAR_ARCHITECTURE_CONSTITUTION.md (Rule 9).
 */
public interface TelemetryContract {

    DeviceTelemetryManager.HardwareStats getHardwareStats(Context context);

    long getAvailableInternalStorageBytes();

    long getTotalInternalStorageBytes();
}

package com.devendra.healthtracker.pomodoro;

import android.content.Context;

/**
 * Public Contract interface for the Pomodoro Capability.
 * Mandated by MODULAR_ARCHITECTURE_CONSTITUTION.md (Rule 9).
 */
public interface PomodoroContract {

    void start(Context context);

    void pause(Context context);

    void reset(Context context);

    void skipToNext(Context context);

    String getStatus(Context context);

    int getRemainingSeconds(Context context);
}

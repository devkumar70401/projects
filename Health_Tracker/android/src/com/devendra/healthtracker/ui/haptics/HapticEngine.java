package com.devendra.healthtracker.ui.haptics;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * HapticEngine
 * Centralized high-fidelity tactile haptic engine leveraging the device's
 * inbuilt linear resonant actuator (LRA) tactile motor.
 * Provides custom calibrated waveform profiles for smooth clicks, subtle ticks,
 * buoyant bubble pops, elemental surges, and gesture transitions.
 */
public final class HapticEngine {

    private static Vibrator vibratorInstance = null;

    private HapticEngine() {}

    private static Vibrator getVibrator(Context context) {
        if (vibratorInstance == null && context != null) {
            try {
                vibratorInstance = (Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Exception ignored) {}
        }
        return vibratorInstance;
    }

    /**
     * Standard crisp button/card tap.
     */
    public static void tap(View view) {
        if (view != null) {
            performViewHaptic(view, HapticFeedbackConstants.KEYBOARD_TAP);
            tap(view.getContext());
        }
    }

    public static void tap(Context context) {
        Vibrator v = getVibrator(context);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(10, 180));
            } else {
                v.vibrate(12);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Featherweight tick for toggles, steppers, sliders, and scroll intervals.
     */
    public static void tick(View view) {
        if (view != null) {
            performViewHaptic(view, HapticFeedbackConstants.CLOCK_TICK);
            tick(view.getContext());
        }
    }

    public static void tick(Context context) {
        Vibrator v = getVibrator(context);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            } else if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(5, 90));
            } else {
                v.vibrate(6);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Solid satisfying heavy click for primary actions, confirms, and resets.
     */
    public static void heavy(View view) {
        if (view != null) {
            performViewHaptic(view, HapticFeedbackConstants.LONG_PRESS);
            heavy(view.getContext());
        }
    }

    public static void heavy(Context context) {
        Vibrator v = getVibrator(context);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
            } else if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(22, 255));
            } else {
                v.vibrate(25);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Fluid buoyant bubble pop tactile impulse.
     * Generates a rapid micro-swell followed by a crisp release pop.
     */
    public static void bubblePop(View view) {
        if (view != null) {
            performViewHaptic(view, HapticFeedbackConstants.CONTEXT_CLICK);
            bubblePop(view.getContext());
        }
    }

    public static void bubblePop(Context context) {
        Vibrator v = getVibrator(context);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));
            } else if (Build.VERSION.SDK_INT >= 26) {
                long[] timings = new long[]{0, 8, 16, 14};
                int[] amps = new int[]{0, 110, 0, 240};
                v.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
            } else {
                v.vibrate(18);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Swipe gesture triggered success feedback (smooth confirmation bump).
     */
    public static void swipeSuccess(View view) {
        if (view != null) {
            swipeSuccess(view.getContext());
        }
    }

    public static void swipeSuccess(Context context) {
        Vibrator v = getVibrator(context);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                long[] timings = new long[]{0, 12, 18, 14};
                int[] amps = new int[]{0, 130, 0, 210};
                v.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
            } else {
                v.vibrate(20);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Elemental card border pulses for fire, thunder, bubbling, air, and volcano.
     */
    public static void elementalPulse(Context context, String element) {
        Vibrator v = getVibrator(context);
        if (v == null || !v.hasVibrator()) return;
        try {
            if ("thunder".equalsIgnoreCase(element)) {
                // Staccato crackling electric arc
                if (Build.VERSION.SDK_INT >= 26) {
                    long[] timings = new long[]{0, 6, 10, 8, 10, 14};
                    int[] amps = new int[]{0, 220, 0, 180, 0, 255};
                    v.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
                } else {
                    v.vibrate(16);
                }
            } else if ("volcano".equalsIgnoreCase(element)) {
                // Deep molten rumbling surge
                if (Build.VERSION.SDK_INT >= 26) {
                    long[] timings = new long[]{0, 15, 8, 20};
                    int[] amps = new int[]{0, 170, 80, 255};
                    v.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
                } else {
                    v.vibrate(26);
                }
            } else if ("fire".equalsIgnoreCase(element)) {
                // Quick flickering flame crackle
                if (Build.VERSION.SDK_INT >= 26) {
                    long[] timings = new long[]{0, 5, 8, 8, 12, 10};
                    int[] amps = new int[]{0, 140, 0, 190, 0, 210};
                    v.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
                } else {
                    v.vibrate(14);
                }
            } else if ("bubbling".equalsIgnoreCase(element) || "water".equalsIgnoreCase(element)) {
                // Rising effervescent ripple
                bubblePop(context);
            } else { // "air"
                // Smooth light breeze swell
                tick(context);
            }
        } catch (Exception ignored) {}
    }

    private static void performViewHaptic(View view, int feedbackConstant) {
        try {
            view.performHapticFeedback(feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING | HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        } catch (Exception e) {
            try {
                view.performHapticFeedback(feedbackConstant);
            } catch (Exception ignored) {}
        }
    }
}

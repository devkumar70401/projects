package com.devendra.mediadownloader.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

/**
 * Centralized theme engine providing VS Code-inspired dark mode and pristine slate light mode.
 * Uses high-performance ARGB integer color tokens to eliminate runtime string parsing overhead.
 * Persists user preference across activity, process, and device reboots.
 */
public class ThemeManager {

    private static final String PREF_NAME = "rose_theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    // 1. Root & Surface Backgrounds
    public static final int BG_LIGHT = 0xFFF8FAFC;      // Slate Porcelain (Slate-50)
    public static final int BG_DARK = 0xFF1E1E1E;       // VS Code Editor Night

    public static final int CARD_BG_LIGHT = 0xFFFFFFFF; // Pure White Card
    public static final int CARD_BG_DARK = 0xFF252526;  // VS Code Sidebar / Surface

    public static final int CARD_ELEVATED_LIGHT = 0xFFF1F5F9; // Slate-100
    public static final int CARD_ELEVATED_DARK = 0xFF2D2D2D;  // VS Code Widget / Elevated

    public static final int CARD_VARIANT_LIGHT = 0xFFF8FAFC;
    public static final int CARD_VARIANT_DARK = 0xFF1F242C;

    public static final int TOPBAR_BG_LIGHT = 0xFFFFFFFF;
    public static final int TOPBAR_BG_DARK = 0xFF181818; // VS Code Titlebar

    // 2. Borders & Dividers
    public static final int BORDER_LIGHT = 0xFFE2E8F0;  // Slate-200
    public static final int BORDER_DARK = 0xFF383838;   // VS Code Panel Muted Border

    public static final int BORDER_SUBTLE_LIGHT = 0xFFCBD5E1; // Slate-300
    public static final int BORDER_SUBTLE_DARK = 0xFF454545;

    public static final int HAIRLINE_LIGHT = 0xFFF1F5F9;
    public static final int HAIRLINE_DARK = 0xFF2D2D2D;

    // 3. Typography
    public static final int TEXT_PRIMARY_LIGHT = 0xFF0F172A; // Slate-900
    public static final int TEXT_PRIMARY_DARK = 0xFFD4D4D4;  // VS Code Foreground (anti-glare)
    public static final int TEXT_BRIGHT_DARK = 0xFFF8FAFC;   // Crisp, high-contrast white for headers and metrics

    public static final int TEXT_SECONDARY_LIGHT = 0xFF64748B; // Slate-500
    public static final int TEXT_SECONDARY_DARK = 0xFFCBD5E1;  // Crisp High-Contrast Slate-300

    public static final int TEXT_MUTED_LIGHT = 0xFF94A3B8;     // Slate-400
    public static final int TEXT_MUTED_DARK = 0xFF94A3B8;      // Clear Slate-400

    // 4. Inputs
    public static final int INPUT_BG_LIGHT = 0xFFFFFFFF;
    public static final int INPUT_BG_DARK = 0xFF2A2D2E;   // VS Code Input box

    public static final int INPUT_BORDER_LIGHT = 0xFFCBD5E1;
    public static final int INPUT_BORDER_DARK = 0xFF3C3C3C;

    public static final int INPUT_TEXT_LIGHT = 0xFF0F172A;
    public static final int INPUT_TEXT_DARK = 0xFFF8FAFC;

    public static final int INPUT_HINT_LIGHT = 0xFF94A3B8;
    public static final int INPUT_HINT_DARK = 0xFF94A3B8;

    // 5. Semantic Accents
    public static final int ACCENT_ROSE_LIGHT = 0xFFE11D48;
    public static final int ACCENT_ROSE_DARK = 0xFFFB7185;

    public static final int ACCENT_EMERALD_LIGHT = 0xFF10B981;
    public static final int ACCENT_EMERALD_DARK = 0xFF34D399;

    public static final int ACCENT_SKY_LIGHT = 0xFF0284C7;
    public static final int ACCENT_SKY_DARK = 0xFF38BDF8;

    public static final int ACCENT_INDIGO_LIGHT = 0xFF6366F1;
    public static final int ACCENT_INDIGO_DARK = 0xFF818CF8;

    public static final int ACCENT_TEAL_LIGHT = 0xFF0D9488;
    public static final int ACCENT_TEAL_DARK = 0xFF2DD4BF;

    public static final int ACCENT_VIOLET_LIGHT = 0xFF7C3AED;
    public static final int ACCENT_VIOLET_DARK = 0xFFA78BFA;

    public static final int ACCENT_AMBER_LIGHT = 0xFFD97706;
    public static final int ACCENT_AMBER_DARK = 0xFFFBBF24;

    private static Boolean cachedDarkMode = null;

    public static boolean isDarkMode(Context context) {
        if (cachedDarkMode != null) {
            return cachedDarkMode;
        }
        if (context == null) return false;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            cachedDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
            return cachedDarkMode;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setDarkMode(Context context, boolean isDark) {
        cachedDarkMode = isDark;
        if (context != null) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply();
            } catch (Exception ignored) {}
        }
    }

    public static boolean toggleTheme(Context context) {
        boolean newState = !isDarkMode(context);
        setDarkMode(context, newState);
        return newState;
    }

    public static void applyStatusBar(Activity activity, boolean isDark) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= 23) {
            activity.getWindow().setStatusBarColor(isDark ? BG_DARK : CARD_BG_LIGHT);
            View decor = activity.getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (isDark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    // --- Accessor Methods ---
    public static int getBackgroundColor(boolean isDark) {
        return isDark ? BG_DARK : BG_LIGHT;
    }

    public static int getCardBackgroundColor(boolean isDark) {
        return isDark ? CARD_BG_DARK : CARD_BG_LIGHT;
    }

    public static int getCardElevatedColor(boolean isDark) {
        return isDark ? CARD_ELEVATED_DARK : CARD_ELEVATED_LIGHT;
    }

    public static int getCardVariantColor(boolean isDark) {
        return isDark ? CARD_VARIANT_DARK : CARD_VARIANT_LIGHT;
    }

    public static int getTopBarBackgroundColor(boolean isDark) {
        return isDark ? TOPBAR_BG_DARK : TOPBAR_BG_LIGHT;
    }

    public static int getBorderColor(boolean isDark) {
        return isDark ? BORDER_DARK : BORDER_LIGHT;
    }

    public static int getBorderSubtleColor(boolean isDark) {
        return isDark ? BORDER_SUBTLE_DARK : BORDER_SUBTLE_LIGHT;
    }

    public static int getHairlineColor(boolean isDark) {
        return isDark ? HAIRLINE_DARK : HAIRLINE_LIGHT;
    }

    public static int getTextPrimaryColor(boolean isDark) {
        return isDark ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT;
    }

    public static int getTextSecondaryColor(boolean isDark) {
        return isDark ? TEXT_SECONDARY_DARK : TEXT_SECONDARY_LIGHT;
    }

    public static int getTextMutedColor(boolean isDark) {
        return isDark ? TEXT_MUTED_DARK : TEXT_MUTED_LIGHT;
    }

    public static int getInputBackgroundColor(boolean isDark) {
        return isDark ? INPUT_BG_DARK : INPUT_BG_LIGHT;
    }

    public static int getInputBorderColor(boolean isDark) {
        return isDark ? INPUT_BORDER_DARK : INPUT_BORDER_LIGHT;
    }

    public static int getInputTextColor(boolean isDark) {
        return isDark ? INPUT_TEXT_DARK : INPUT_TEXT_LIGHT;
    }

    public static int getInputHintColor(boolean isDark) {
        return isDark ? INPUT_HINT_DARK : INPUT_HINT_LIGHT;
    }

    public static int parseColorHex(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            String clean = hex.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() == 6) {
                return 0xFF000000 | (int) Long.parseLong(clean, 16);
            } else if (clean.length() == 8) {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    public static int getBadgeBackground(boolean isDark, String accentHex) {
        int accent = parseColorHex(accentHex, 0xFFE11D48);
        if (!isDark) {
            // Light pastel tint: 12% opacity of accent on white
            int r = (accent >> 16) & 0xFF;
            int g = (accent >> 8) & 0xFF;
            int b = accent & 0xFF;
            int blendedR = (int) (255 * 0.90f + r * 0.10f);
            int blendedG = (int) (255 * 0.90f + g * 0.10f);
            int blendedB = (int) (255 * 0.90f + b * 0.10f);
            return 0xFF000000 | (blendedR << 16) | (blendedG << 8) | blendedB;
        } else {
            // Dark mode: 20% opacity tint
            int r = (accent >> 16) & 0xFF;
            int g = (accent >> 8) & 0xFF;
            int b = accent & 0xFF;
            int blendedR = (int) (0x25 * 0.80f + r * 0.20f);
            int blendedG = (int) (0x25 * 0.80f + g * 0.20f);
            int blendedB = (int) (0x26 * 0.80f + b * 0.20f);
            return 0xFF000000 | (blendedR << 16) | (blendedG << 8) | blendedB;
        }
    }

    public static int getBadgeBorder(boolean isDark, String accentHex) {
        int accent = parseColorHex(accentHex, 0xFFE11D48);
        if (!isDark) {
            int r = (accent >> 16) & 0xFF;
            int g = (accent >> 8) & 0xFF;
            int b = accent & 0xFF;
            int blendedR = (int) (255 * 0.70f + r * 0.30f);
            int blendedG = (int) (255 * 0.70f + g * 0.30f);
            int blendedB = (int) (255 * 0.70f + b * 0.30f);
            return 0xFF000000 | (blendedR << 16) | (blendedG << 8) | blendedB;
        } else {
            int r = (accent >> 16) & 0xFF;
            int g = (accent >> 8) & 0xFF;
            int b = accent & 0xFF;
            int blendedR = (int) (0x38 * 0.65f + r * 0.35f);
            int blendedG = (int) (0x38 * 0.65f + g * 0.35f);
            int blendedB = (int) (0x38 * 0.65f + b * 0.35f);
            return 0xFF000000 | (blendedR << 16) | (blendedG << 8) | blendedB;
        }
    }
}

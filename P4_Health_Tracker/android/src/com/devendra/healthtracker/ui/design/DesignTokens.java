package com.devendra.healthtracker.ui.design;

import android.content.Context;
import android.graphics.Color;

/**
 * Centralized design tokens establishing the "Premium Soft-Tactile Minimal" design system.
 * Defines semantic roles, corner radius scales, touch targets, elevation, and motion curves.
 */
public final class DesignTokens {

    private DesignTokens() {}

    // ==========================================
    // 1. CORNER RADIUS SCALE (dp)
    // ==========================================
    public static final int RADIUS_XS = 6;      // Micro tags, sub-chips
    public static final int RADIUS_SM = 10;     // Buttons, input fields, chips
    public static final int RADIUS_MD = 14;     // Medium cards, keypad keys, dialogs
    public static final int RADIUS_LG = 18;     // Feature tiles, primary cards
    public static final int RADIUS_XL = 24;     // Hero cards, bottom sheets
    public static final int RADIUS_PILL = 999;  // Toggles, segmented pills, circular badges

    // ==========================================
    // 2. TOUCH TARGETS & SIZING (dp)
    // ==========================================
    public static final int MIN_TOUCH_TARGET = 48; // Android WCAG 2.1 touch accessibility
    public static final int BUTTON_HEIGHT_LG = 48; // Hero / Primary actions
    public static final int BUTTON_HEIGHT_MD = 40; // Standard form & modal buttons
    public static final int BUTTON_HEIGHT_SM = 32; // Compact row actions, chips
    public static final int BUTTON_HEIGHT_XS = 26; // Micro badges / memory keys

    public static final int ICON_SIZE_LG = 24;
    public static final int ICON_SIZE_MD = 20;
    public static final int ICON_SIZE_SM = 16;

    // ==========================================
    // 3. ELEVATION TOKENS (dp)
    // ==========================================
    public static final float ELEVATION_FLAT = 0.0f;
    public static final float ELEVATION_SUBTLE = 1.5f;
    public static final float ELEVATION_CARD = 2.0f;
    public static final float ELEVATION_PRESSED = 0.5f;
    public static final float ELEVATION_FLOATING = 6.0f;

    // ==========================================
    // 4. ANIMATION TIMINGS & PHYSICS (ms)
    // ==========================================
    public static final int DURATION_PRESS_DOWN = 90;   // Instant tactile compression
    public static final int DURATION_PRESS_UP = 180;    // Elastic spring recovery
    public static final int DURATION_TRANSITION = 220;  // Smooth state morphing
    public static final int DURATION_PAGE_FLIP = 280;

    public static final float SCALE_PRESSED_BUTTON = 0.965f;  // Subtle tactile depression
    public static final float SCALE_PRESSED_CARD = 0.985f;    // Refined card compression
    public static final float SCALE_PRESSED_KEY = 0.950f;     // Crisp keypad response

    // ==========================================
    // 5. SEMANTIC PALETTES (Light / Dark)
    // ==========================================

    // Neutral Surfaces (Obsidian Midnight Architecture)
    public static int getSurface(boolean isDark) {
        return isDark ? 0xFF0B0E14 : 0xFFF8FAFC;
    }

    public static int getSurfaceRoot(boolean isDark) {
        return getSurface(isDark);
    }

    public static int getSurfaceCard(boolean isDark) {
        return isDark ? 0xFF141923 : 0xFFFFFFFF;
    }

    public static int getSurfaceElevated(boolean isDark) {
        return isDark ? 0xFF1B2232 : 0xFFF1F5F9;
    }

    public static int getSurfaceInput(boolean isDark) {
        return isDark ? 0xFF121722 : 0xFFFFFFFF;
    }

    public static int getSurfacePressed(boolean isDark) {
        return isDark ? 0xFF242C42 : 0xFFE2E8F0;
    }

    public static int getSurfaceVariant(boolean isDark) {
        return isDark ? 0xFF0E121A : 0xFFF8FAFC;
    }

    // Borders & Hairlines
    public static int getBorder(boolean isDark) {
        return isDark ? 0xFF232C3F : 0xFFE2E8F0;
    }

    public static int getBorderSubtle(boolean isDark) {
        return isDark ? 0xFF283348 : 0xFFCBD5E1;
    }

    public static int getBorderHairline(boolean isDark) {
        return isDark ? 0xFF171D2A : 0xFFF1F5F9;
    }

    // Typography
    public static final int DARK_TEXT_PRIMARY = 0xFFF8FAFC;   // Pristine, high-contrast crisp white (Slate-50)
    public static final int DARK_TEXT_SECONDARY = 0xFF94A3B8; // Elegant, readable light slate (Slate-400)
    public static final int DARK_TEXT_MUTED = 0xFF64748B;     // Slate-500

    public static final int LIGHT_TEXT_PRIMARY = 0xFF0F172A;
    public static final int LIGHT_TEXT_SECONDARY = 0xFF64748B;
    public static final int LIGHT_TEXT_MUTED = 0xFF94A3B8;

    public static int getTextPrimary(boolean isDark) {
        return isDark ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
    }

    public static int getTextSecondary(boolean isDark) {
        return isDark ? DARK_TEXT_SECONDARY : LIGHT_TEXT_SECONDARY;
    }

    public static int getTextMuted(boolean isDark) {
        return isDark ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED;
    }

    // Semantic Accents
    // Primary / Rose (Hero action, timer, routine)
    public static final int ACCENT_ROSE_PRIMARY = 0xFFE11D48;
    public static final int ACCENT_ROSE_HOVER = 0xFFBE123C;
    public static final int ACCENT_ROSE_DARK = 0xFFFB7185;
    public static final int ACCENT_ROSE_CONTAINER_LIGHT = 0xFFFFF1F2;
    public static final int ACCENT_ROSE_CONTAINER_DARK = 0xFF2A141D;

    // Emerald (Success, checklist, optimal)
    public static final int ACCENT_EMERALD_PRIMARY = 0xFF10B981;
    public static final int ACCENT_EMERALD_DARK = 0xFF34D399;
    public static final int ACCENT_EMERALD_CONTAINER_LIGHT = 0xFFECFDF5;
    public static final int ACCENT_EMERALD_CONTAINER_DARK = 0xFF10261E;

    // Sky / Cyan (Hydration, info, secondary telemetry)
    public static final int ACCENT_SKY_PRIMARY = 0xFF0284C7;
    public static final int ACCENT_SKY_DARK = 0xFF38BDF8;
    public static final int ACCENT_SKY_CONTAINER_LIGHT = 0xFFF0F9FF;
    public static final int ACCENT_SKY_CONTAINER_DARK = 0xFF0E2232;

    // Indigo / Violet (System monitor, downloader)
    public static final int ACCENT_INDIGO_PRIMARY = 0xFF6366F1;
    public static final int ACCENT_INDIGO_DARK = 0xFF818CF8;
    public static final int ACCENT_INDIGO_CONTAINER_LIGHT = 0xFFEEF2FF;
    public static final int ACCENT_INDIGO_CONTAINER_DARK = 0xFF1A1C3C;

    // Violet
    public static final int ACCENT_VIOLET_PRIMARY = 0xFF7C3AED;
    public static final int ACCENT_VIOLET_DARK = 0xFFA78BFA;
    public static final int ACCENT_VIOLET_CONTAINER_LIGHT = 0xFFF5F3FF;
    public static final int ACCENT_VIOLET_CONTAINER_DARK = 0xFF201738;

    // Teal (Deep cleaner, purge)
    public static final int ACCENT_TEAL_PRIMARY = 0xFF0D9488;
    public static final int ACCENT_TEAL_DARK = 0xFF2DD4BF;
    public static final int ACCENT_TEAL_CONTAINER_LIGHT = 0xFFF0FDFA;
    public static final int ACCENT_TEAL_CONTAINER_DARK = 0xFF0F2624;

    // Amber (Calculator, warning)
    public static final int ACCENT_AMBER_PRIMARY = 0xFFD97706;
    public static final int ACCENT_AMBER_DARK = 0xFFFBBF24;
    public static final int ACCENT_AMBER_CONTAINER_LIGHT = 0xFFFFFBEB;
    public static final int ACCENT_AMBER_CONTAINER_DARK = 0xFF2E2310;

    // Destructive
    public static final int DESTRUCTIVE_PRIMARY = 0xFFDC2626;
    public static final int DESTRUCTIVE_DARK = 0xFFF87171;
    public static final int DESTRUCTIVE_CONTAINER_LIGHT = 0xFFFEF2F2;
    public static final int DESTRUCTIVE_CONTAINER_DARK = 0xFF36141A;

    // Helper to calculate DP to Pixels dynamically
    public static int dp(Context context, int dpVal) {
        if (context == null) return dpVal * 2;
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dpVal * density);
    }
}

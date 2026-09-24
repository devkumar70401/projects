package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.theme.ThemeManager;

public class ThemeManagerTest {

    public static void run() {
        System.out.println("--------------------------------------------------");
        System.out.println("Running ThemeManagerTest (Obsidian Midnight & Slate Light)...");
        System.out.println("--------------------------------------------------");

        // 1. Light Mode Tokens Test
        int lightBg = ThemeManager.getBackgroundColor(false);
        int lightCard = ThemeManager.getCardBackgroundColor(false);
        int lightText = ThemeManager.getTextPrimaryColor(false);
        int lightBorder = ThemeManager.getBorderColor(false);

        assertEq(lightBg, 0xFFF8FAFC, "Light mode background should be slate porcelain (#F8FAFC)");
        assertEq(lightCard, 0xFFFFFFFF, "Light mode card background should be white (#FFFFFF)");
        assertEq(lightText, 0xFF0F172A, "Light mode text primary should be slate-900 (#0F172A)");
        assertEq(lightBorder, 0xFFE2E8F0, "Light mode border should be slate-200 (#E2E8F0)");
        System.out.println("  ✓ Light theme tokens verified");

        // 2. Dark Mode Tokens Test (Obsidian Midnight luxury character)
        int darkBg = ThemeManager.getBackgroundColor(true);
        int darkCard = ThemeManager.getCardBackgroundColor(true);
        int darkText = ThemeManager.getTextPrimaryColor(true);
        int darkBorder = ThemeManager.getBorderColor(true);

        assertEq(darkBg, 0xFF0B0E14, "Dark mode background must match Obsidian Midnight (#0B0E14)");
        assertEq(darkCard, 0xFF141923, "Dark mode card background must match Midnight Slate (#141923)");
        assertEq(darkText, 0xFFF8FAFC, "Dark mode text primary must be radiant crisp Slate-50 (#F8FAFC)");
        assertEq(darkBorder, 0xFF232C3F, "Dark mode border must be sleek sapphire-slate hairline (#232C3F)");
        System.out.println("  ✓ Dark theme Obsidian Midnight tokens verified");

        // 3. Contrast & Hierarchy verification
        assertTrue(darkBg != 0xFF000000, "Dark mode background must not be pitch black #000000 to prevent OLED smear");
        assertTrue(darkCard != 0xFF000000, "Dark mode card must not be pitch black #000000");
        assertTrue(darkCard != darkBg, "Card surface must be distinct from root background");
        System.out.println("  ✓ Surface hierarchy and anti-smear contrast verified");

        // 4. Badges & Chip contrast in Light and Dark
        int lightBadgeBg = ThemeManager.getBadgeBackground(false, "#E11D48");
        int darkBadgeBg = ThemeManager.getBadgeBackground(true, "#E11D48");
        assertTrue(lightBadgeBg != darkBadgeBg, "Badge background must adapt between light and dark modes");
        System.out.println("  ✓ Semantic badge tokens verified");

        System.out.println("✅ All ThemeManagerTest assertions PASSED!");
    }

    private static void assertEq(int actual, int expected, String msg) {
        if (actual != expected) {
            throw new AssertionError("FAIL: " + msg + " [Expected: " + String.format("#%08X", expected) + ", Got: " + String.format("#%08X", actual) + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("FAIL: " + msg);
    }
}

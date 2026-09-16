package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.features.FeatureItem;
import com.devendra.healthtracker.features.FeatureRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeatureOrderManagerTest {

    public static class TestOrderValidator {
        public static List<String> sanitizeOrder(List<String> rawOrder, List<String> defaultOrder) {
            if (rawOrder == null || rawOrder.isEmpty()) {
                return new ArrayList<>(defaultOrder);
            }

            List<String> validated = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (String id : rawOrder) {
                if (id != null && !id.trim().isEmpty() && FeatureRegistry.hasFeature(id) && !seen.contains(id)) {
                    validated.add(id);
                    seen.add(id);
                }
            }

            // Append any missing features
            for (String defId : defaultOrder) {
                if (!seen.contains(defId)) {
                    validated.add(defId);
                    seen.add(defId);
                }
            }

            if (validated.isEmpty()) {
                return new ArrayList<>(defaultOrder);
            }

            return validated;
        }
    }

    public static void run() {
        System.out.println("--------------------------------------------------");
        System.out.println("Running FeatureOrderManagerTest...");
        System.out.println("--------------------------------------------------");

        List<String> defaultOrder = FeatureRegistry.getDefaultOrder();
        assertEq(defaultOrder.size(), 3, "Active registry must contain exactly 3 features: Routine, Checklist, Cleaner");
        System.out.println("  ✓ Default active order count is 3");

        // 1. Verify all 3 features exist with valid metadata
        for (String id : defaultOrder) {
            FeatureItem item = FeatureRegistry.getFeature(id);
            assertNotNull(item, "Feature " + id + " must exist in registry");
            assertNotNull(item.getShortName(), "Feature short name must not be null");
            assertNotNull(item.getAccentColorHex(), "Feature accent color must not be null");
            assertTrue(!item.getShortName().trim().isEmpty(), "Feature short name must not be empty");
        }
        System.out.println("  ✓ All 3 active feature definitions verified");

        // 2. Test reordering preservation (e.g. cleaner moved to first)
        List<String> customOrder = new ArrayList<>(defaultOrder);
        customOrder.remove(FeatureRegistry.ID_DEEPCLEAN);
        customOrder.add(0, FeatureRegistry.ID_DEEPCLEAN);

        List<String> sanitizedCustom = TestOrderValidator.sanitizeOrder(customOrder, defaultOrder);
        assertEq(sanitizedCustom.size(), 3, "Custom order size must remain 3");
        assertEq(sanitizedCustom.get(0), FeatureRegistry.ID_DEEPCLEAN, "First item must be cleaner");
        System.out.println("  ✓ Custom reordering correctly preserved");

        // 3. Test handling of duplicate IDs
        List<String> duplicateOrder = new ArrayList<>();
        duplicateOrder.add(FeatureRegistry.ID_ROUTINE);
        duplicateOrder.add(FeatureRegistry.ID_ROUTINE);
        duplicateOrder.add(FeatureRegistry.ID_CHECKLIST);

        List<String> sanitizedDuplicates = TestOrderValidator.sanitizeOrder(duplicateOrder, defaultOrder);
        assertEq(sanitizedDuplicates.size(), 3, "Deduplicated order must contain all 3 features once");
        assertEq(sanitizedDuplicates.get(0), FeatureRegistry.ID_ROUTINE, "First item must be routine");
        assertEq(sanitizedDuplicates.get(1), FeatureRegistry.ID_CHECKLIST, "Second item must be checklist");

        Set<String> uniqueCheck = new HashSet<>(sanitizedDuplicates);
        assertEq(uniqueCheck.size(), 3, "No duplicates allowed in sanitized order");
        System.out.println("  ✓ Duplicates safely filtered and missing features appended");

        // 4. Test obsolete / disabled ID handling (e.g. disabled ID_MEDIA must be stripped)
        List<String> obsoleteOrder = new ArrayList<>();
        obsoleteOrder.add("obsolete_feature_xyz");
        obsoleteOrder.add(FeatureRegistry.ID_MEDIA); // disabled feature
        obsoleteOrder.add(FeatureRegistry.ID_CHECKLIST);

        List<String> sanitizedObsolete = TestOrderValidator.sanitizeOrder(obsoleteOrder, defaultOrder);
        assertTrue(!sanitizedObsolete.contains("obsolete_feature_xyz"), "Obsolete IDs must be stripped");
        assertTrue(!sanitizedObsolete.contains(FeatureRegistry.ID_MEDIA), "Disabled IDs must be stripped");
        assertEq(sanitizedObsolete.get(0), FeatureRegistry.ID_CHECKLIST, "Valid ID checklist must be retained first");
        assertEq(sanitizedObsolete.size(), 3, "Total size must still equal active registry size (3)");
        System.out.println("  ✓ Obsolete & disabled feature IDs discarded safely");

        // 5. Test empty or null fallback
        List<String> emptyFallback = TestOrderValidator.sanitizeOrder(new ArrayList<>(), defaultOrder);
        assertEq(emptyFallback.size(), 3, "Empty list must safely fallback to default order");

        List<String> nullFallback = TestOrderValidator.sanitizeOrder(null, defaultOrder);
        assertEq(nullFallback.size(), 3, "Null list must safely fallback to default order");
        System.out.println("  ✓ Empty and null order safely recovers to default");

        System.out.println("✅ All FeatureOrderManagerTest assertions PASSED!");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (!actual.equals(expected)) {
            throw new AssertionError("FAIL: " + msg + " [Expected: " + expected + ", Got: " + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("FAIL: " + msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError("FAIL: " + msg + " (was null)");
    }
}

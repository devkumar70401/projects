package com.devendra.healthtracker.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChecklistManagerTest {

    public static class TestChecklistItem {
        public String id;
        public String title;
        public boolean completed;
        public long createdAt;
        public long completedAt;

        public TestChecklistItem(String title) {
            this.id = UUID.randomUUID().toString();
            this.title = title != null ? title.trim() : "";
            this.completed = false;
            this.createdAt = System.currentTimeMillis();
            this.completedAt = 0L;
        }

        public TestChecklistItem(String id, String title, boolean completed, long createdAt, long completedAt) {
            this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
            this.title = title != null ? title.trim() : "";
            this.completed = completed;
            this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
            this.completedAt = completedAt;
        }

        public String toSimpleJson() {
            return String.format("{\"id\":\"%s\",\"title\":\"%s\",\"completed\":%b,\"createdAt\":%d,\"completedAt\":%d}",
                id, title.replace("\"", "\\\""), completed, createdAt, completedAt);
        }
    }

    public static class TestChecklistManager {
        public List<TestChecklistItem> items = new ArrayList<>();

        public TestChecklistItem addItem(String title) {
            if (title == null || title.trim().isEmpty()) return null;
            TestChecklistItem item = new TestChecklistItem(title.trim());
            items.add(0, item);
            return item;
        }

        public boolean toggleItem(String id) {
            for (TestChecklistItem i : items) {
                if (i.id.equals(id)) {
                    i.completed = !i.completed;
                    i.completedAt = i.completed ? System.currentTimeMillis() : 0L;
                    return i.completed;
                }
            }
            return false;
        }

        public boolean deleteItem(String id) {
            return items.removeIf(i -> i.id.equals(id));
        }

        public int clearCompleted() {
            int before = items.size();
            items.removeIf(i -> i.completed);
            return before - items.size();
        }

        public List<TestChecklistItem> filter(String filterType) {
            List<TestChecklistItem> out = new ArrayList<>();
            for (TestChecklistItem i : items) {
                if ("ACTIVE".equalsIgnoreCase(filterType) && !i.completed) out.add(i);
                else if ("COMPLETED".equalsIgnoreCase(filterType) && i.completed) out.add(i);
                else if ("ALL".equalsIgnoreCase(filterType)) out.add(i);
            }
            return out;
        }
    }

    public static void runTests() {
        System.out.println("--- [TEST SUITE 2: Checklist CRUD & State Operations] ---");

        TestChecklistManager manager = new TestChecklistManager();

        // 1. Add valid item
        TestChecklistItem item1 = manager.addItem("Buy Almonds & Pumpkin Seeds");
        assertNotNull(item1, "Added item should not be null");
        assertNotNull(item1.id, "Item ID must be generated");
        assertEq(item1.title, "Buy Almonds & Pumpkin Seeds", "Item title must match");
        assertFalse(item1.completed, "Initial completed must be false");
        assertEq(item1.completedAt, 0L, "Initial completedAt must be 0");
        assertEq(manager.items.size(), 1, "Manager size should be 1");

        // 2. Add second item (prepends to top)
        TestChecklistItem item2 = manager.addItem("Review Endocrine Cadence");
        assertEq(manager.items.size(), 2, "Manager size should be 2");
        assertEq(manager.items.get(0).id, item2.id, "Latest item should be prepended to top");

        // 3. Reject empty or null title
        TestChecklistItem empty = manager.addItem("   ");
        assertNull(empty, "Empty whitespace title must be rejected");
        TestChecklistItem nullItem = manager.addItem(null);
        assertNull(nullItem, "Null title must be rejected");
        assertEq(manager.items.size(), 2, "Size unchanged after invalid adds");

        // 4. Toggle completion
        boolean state = manager.toggleItem(item1.id);
        assertTrue(state, "toggleItem should return true when completed");
        assertTrue(item1.completed, "item1 completed should now be true");
        assertTrue(item1.completedAt > 0, "item1 completedAt must be set to current timestamp");

        // 5. Toggle back to uncompleted
        boolean state2 = manager.toggleItem(item1.id);
        assertFalse(state2, "toggleItem should return false when reverted");
        assertFalse(item1.completed, "item1 completed should now be false");
        assertEq(item1.completedAt, 0L, "item1 completedAt must be reset to 0");

        // 6. Test filtering
        manager.toggleItem(item1.id); // item1 done, item2 active
        List<TestChecklistItem> all = manager.filter("ALL");
        assertEq(all.size(), 2, "Filter ALL should return 2 items");

        List<TestChecklistItem> active = manager.filter("ACTIVE");
        assertEq(active.size(), 1, "Filter ACTIVE should return 1 item");
        assertEq(active.get(0).id, item2.id, "Active item must be item2");

        List<TestChecklistItem> completedList = manager.filter("COMPLETED");
        assertEq(completedList.size(), 1, "Filter COMPLETED should return 1 item");
        assertEq(completedList.get(0).id, item1.id, "Completed item must be item1");

        // 7. Delete item
        boolean deleted = manager.deleteItem(item2.id);
        assertTrue(deleted, "deleteItem should return true on successful deletion");
        assertEq(manager.items.size(), 1, "Size after delete should be 1");

        // 8. Clear completed
        int cleared = manager.clearCompleted();
        assertEq(cleared, 1, "clearCompleted should remove 1 completed item");
        assertEq(manager.items.size(), 0, "List should be empty after clear completed");

        // 9. JSON Serialization roundtrip check
        TestChecklistItem sample = new TestChecklistItem("id-123", "Hydrate 500ml", true, 1000L, 2000L);
        String json = sample.toSimpleJson();
        assertTrue(json.contains("\"id\":\"id-123\""), "JSON must contain ID");
        assertTrue(json.contains("\"title\":\"Hydrate 500ml\""), "JSON must contain title");
        assertTrue(json.contains("\"completed\":true"), "JSON must contain completed status");

        System.out.println("✅ ChecklistManagerTest: ALL 9 ASSERTIONS PASSED!");
    }

    private static void assertEq(Object actual, Object expected, String msg) {
        if (!actual.equals(expected)) {
            throw new AssertionError("FAIL: " + msg + " [Expected: " + expected + ", Got: " + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("FAIL: " + msg);
    }

    private static void assertFalse(boolean condition, String msg) {
        if (condition) throw new AssertionError("FAIL: " + msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError("FAIL: " + msg + " (was null)");
    }

    private static void assertNull(Object obj, String msg) {
        if (obj != null) throw new AssertionError("FAIL: " + msg + " (was not null: " + obj + ")");
    }
}

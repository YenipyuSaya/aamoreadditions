package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class WrenchPlannerTest {
    private static final Predicate<String> ALL = i -> true;
    private static List<String> slots(String... items) { return Arrays.asList(items); }
    private static boolean valid(List<String> slots) {
        Set<String> have = new HashSet<>();
        for (String item : slots) if (item != null) {
            if (!have.add(item)) return false;
        }
        for (String item : have) {
            int tier = item.charAt(1) - '0';
            for (int i = 1; i < tier; i++) if (!have.contains(item.substring(0, 1) + i)) return false;
        }
        return true;
    }
    private static List<String> plan(List<String> record, List<String> current, Map<String, Integer> stock) {
        return WrenchPlanner.plan(record, current, ALL, stock, false, WrenchPlannerTest::valid);
    }

    @Test void exactPositionsNeverCompactWhenItemMissing() {
        assertEquals(slots(null, "e1", null, null, null), plan(
                slots("s1", "e1", null, null, null), slots(null, null, null, null, null), Map.of("e1", 1)));
    }
    @Test void missingPrerequisiteKeepsOldItemAndDoesNotMoveLaterUpgrade() {
        assertEquals(slots(null, "e1", null, null, null), plan(
                slots("s1", "s2", null, null, null), slots(null, "e1", null, null, null), Map.of("s2", 1)));
    }
    @Test void missingReplacementPreservesOldItem() {
        assertEquals(slots("e1", null), plan(slots("s1", null), slots("e1", null), Map.of()));
    }
    @Test void recordedEmptyClearsMatchingSlotCount() {
        assertEquals(slots(null, null), plan(slots(null, null), slots("s1", "s2"), Map.of()));
    }
    @Test void cannotClearPrerequisiteWhileDependentIsRetained() {
        assertEquals(slots("s1", "s2"), plan(slots(null, "s2"), slots("s1", "s2"), Map.of()));
    }
    @Test void canRemoveWholeDependencyChainInOneTransaction() {
        assertEquals(slots(null, null, null), plan(slots(null, null, null), slots("s1", "s2", "s3"), Map.of()));
    }
    @Test void replacementsThatBreakRetainedDependencyAreSkipped() {
        assertEquals(slots("s1", "s2"), plan(slots("e1", "s2"), slots("s1", "s2"), Map.of("e1", 1)));
    }
    @Test void compatibleReplacementApplies() {
        assertEquals(slots("s1", null), plan(slots("s1", null), slots("e1", null), Map.of("s1", 1)));
    }
    @Test void swappedMachineItemsAreReusedWithoutBackpackCopies() {
        assertEquals(slots("s2", "s1"), plan(slots("s2", "s1"), slots("s1", "s2"), Map.of()));
    }
    @Test void prerequisiteMayBeInALaterTargetSlot() {
        assertEquals(slots("s3", "s2", "s1"), plan(slots("s3", "s2", "s1"), slots(null, null, null),
                Map.of("s1", 1, "s2", 1, "s3", 1)));
    }
    @Test void incompatibleSameSizeRecordPreservesExistingSlot() {
        assertEquals(slots("e1", null), WrenchPlanner.plan(slots("s1", null), slots("e1", null),
                i -> i.startsWith("e"), Map.of("s1", 1), false, WrenchPlannerTest::valid));
    }
    @Test void crossSizeSkipsEmptyAndIncompatibleThenKeepsUncoveredSlots() {
        assertEquals(slots("e1", "e2"), WrenchPlanner.plan(slots(null, "s1", "e1", null, null), slots(null, "e2"),
                i -> i.startsWith("e"), Map.of("e1", 1), false, WrenchPlannerTest::valid));
    }
    @Test void crossSizeAllEmptyIsNoOp() {
        assertEquals(slots("s1", "s2"), plan(slots(null, null, null, null, null), slots("s1", "s2"), Map.of()));
    }
    @Test void crossSizeMissingInventoryReservesItsMappedPosition() {
        assertEquals(slots("e1", "e2", null), plan(slots("s1", "e2", null, null, null),
                slots("e1", null, null), Map.of("e2", 1)));
    }
    @Test void crossSizeMissingPrerequisitePreservesOldAndDoesNotCompact() {
        assertEquals(slots("e1", "e2", null), plan(slots("s2", "e2", null, null, null),
                slots("e1", "e2", null), Map.of("s2", 1)));
    }
    @Test void crossSizeTruncatesOnlyAfterFiltering() {
        assertEquals(slots("e1", "e2"), WrenchPlanner.plan(slots("s1", null, "e1", "e2", "s2"), slots(null, null),
                i -> i.startsWith("e"), Map.of("e1", 1, "e2", 1), false, WrenchPlannerTest::valid));
    }
    @Test void creativeNeedsNoStockButStillRequiresPrerequisites() {
        assertEquals(slots("s1", "s2"), WrenchPlanner.plan(slots("s1", "s2"), slots(null, null),
                ALL, Map.of(), true, WrenchPlannerTest::valid));
        assertEquals(slots(null, "e1"), WrenchPlanner.plan(slots("s2", "e1"), slots(null, null),
                ALL, Map.of(), true, WrenchPlannerTest::valid));
    }
    @Test void neverDuplicatesItemNeededInRetainedSlot() {
        assertEquals(slots(null, "s1"), plan(slots("s1", "s2"), slots(null, "s1"), Map.of()));
    }
    @Test void threeWayCycleRequiresFinalStateRatherThanSequentialValidation() {
        assertEquals(slots("s3", "s1", "s2"), plan(slots("s3", "s1", "s2"), slots("s1", "s2", "s3"), Map.of()));
    }
    @Test void identicalRecordNeedsNoInventoryAndNoChange() {
        List<String> before = slots("s2", "s1", null);
        assertFalse(WrenchPlanner.changed(before, plan(before, before, Map.of())));
    }
    @Test void partialSuccessAndRemovalAreChangesButFailedApplyIsNot() {
        var before = slots(null, null);
        assertTrue(WrenchPlanner.changed(before, plan(slots("s1", "e1"), before, Map.of("s1", 1))));
        assertFalse(WrenchPlanner.changed(before, plan(slots("s1", "e1"), before, Map.of())));
        assertTrue(WrenchPlanner.changed(slots("e1", null), plan(slots(null, null), slots("e1", null), Map.of())));
    }
    @Test void plannerDoesNotMutateInputs() {
        var record = slots("s2", "s1"); var current = slots("s1", "s2"); var stock = new HashMap<String, Integer>();
        plan(record, current, stock);
        assertEquals(slots("s2", "s1"), record); assertEquals(slots("s1", "s2"), current); assertTrue(stock.isEmpty());
    }
    @Test void invalidExternalConfigurationIsNotDestroyedWhenNoValidPlanExists() {
        assertEquals(slots("s2", null), plan(slots("s2", "s3"), slots("s2", null), Map.of()));
    }
    @Test void rejectsUnboundedSlotCounts() {
        assertThrows(IllegalArgumentException.class, () -> plan(slots("s1"), List.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> plan(slots("s1"), slots(null,null,null,null,null,null), Map.of()));
    }
    @Test void fullBackpackStillPermitsPureMachineReordering() {
        // Capacity of the backpack is irrelevant: the machine's own real items are reserved first.
        assertEquals(slots("e1", "s1"), plan(slots("e1", "s1"), slots("s1", "e1"), Map.of()));
    }
    @Test void incompatibleRecordCannotGenerateCreativeUpgrade() {
        assertEquals(slots("e1", null), WrenchPlanner.plan(slots("s1", null), slots("e1", null),
                i -> i.startsWith("e"), Map.of(), true, WrenchPlannerTest::valid));
    }
    @Test void clearAndRefillCanReuseMachinePluginWithoutDuplicate() {
        assertEquals(slots(null, "s1"), plan(slots(null, "s1"), slots("s1", null), Map.of()));
    }
    @Test void randomizedValidFinalStatesNeverCreateItemsOrInventUnrequestedChanges() {
        Random random = new Random(73126);
        String[] kinds = {null, "s1", "s2", "s3", "e1", "e2"};
        for (int trial = 0; trial < 3000; trial++) {
            int size = 1 + random.nextInt(5);
            List<String> before = new ArrayList<>();
            do {
                before.clear(); for (int i = 0; i < size; i++) before.add(kinds[random.nextInt(kinds.length)]);
            } while (!valid(before));
            List<String> record = new ArrayList<>();
            for (int i = 0; i < size; i++) record.add(kinds[random.nextInt(kinds.length)]);
            Map<String, Integer> inventory = new HashMap<>();
            for (String k : kinds) if (k != null) inventory.put(k, random.nextInt(2));
            List<String> after = plan(record, before, inventory);
            assertTrue(valid(after), "Illegal final state: " + after);
            for (int i = 0; i < size; i++) assertTrue(java.util.Objects.equals(after.get(i), before.get(i))
                    || java.util.Objects.equals(after.get(i), record.get(i)));
            for (String k : kinds) if (k != null) assertTrue(Collections.frequency(after, k)
                    <= Collections.frequency(before, k) + inventory.getOrDefault(k, 0));
        }
    }
}

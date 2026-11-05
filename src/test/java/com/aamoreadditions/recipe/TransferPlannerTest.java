package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TransferPlannerTest {
    static final Predicate<String> COPPER = s -> Set.of("copper", "othermod_copper").contains(s), TIN = "tin"::equals, ZINC = "zinc"::equals;

    static TransferPlanner.Plan plan(String id, List<Predicate<String>> ings, int[] counts, RecipeLock lock, String[] items, int[] cnt) {
        return TransferPlanner.plan(id, ings, counts, lock, Arrays.asList(items), cnt, Objects::equals, s -> 64);
    }

    @Test void unlockedInOrder() {
        var p = plan("create:brass", List.of(COPPER, ZINC), new int[]{1, 1}, new RecipeLock(),
                new String[]{"zinc", "copper", null}, new int[]{10, 10, 0});
        assertEquals(TransferPlanner.Status.OK, p.status());
        assertArrayEquals(new int[]{0, 1, -1}, p.slotIngredient());
        assertArrayEquals(new int[]{1, 1, 0}, p.slotAmount());
        assertArrayEquals(new int[]{1, 0, -1}, p.slotSource()); // copper from pool 1, zinc from pool 0
        assertArrayEquals(new int[]{1, 1, 0}, p.take());
    }

    @Test void pendingLockActsUnlocked() {
        RecipeLock l = new RecipeLock(); l.toggle();
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, l, new String[]{"copper", "tin"}, new int[]{5, 5});
        assertArrayEquals(new int[]{0, 1, -1}, p.slotIngredient());
        assertArrayEquals(new int[]{3, 1, 0}, p.slotAmount());
    }

    static RecipeLock lockedBronzeSplit() {
        // recorded with copper in input 1 + extra, tin in input 2
        RecipeLock l = new RecipeLock(); l.toggle();
        l.recordIfPending("x:bronze", new int[][]{{1, 0, 2}, {0, 1, 0}});
        return l;
    }

    @Test void lockedSameRecipeUsesMappingAndSplits() {
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, lockedBronzeSplit(), new String[]{"copper", "tin"}, new int[]{3, 1});
        assertEquals(TransferPlanner.Status.OK, p.status());
        assertArrayEquals(new int[]{0, 1, 0}, p.slotIngredient());
        assertArrayEquals(new int[]{2, 1, 1}, p.slotAmount()); // 3 copper over 2 slots: 2 + 1
        assertArrayEquals(new int[]{3, 1}, p.take());
    }

    @Test void lockedOtherRecipeIgnored() {
        var p = plan("create:brass", List.of(COPPER, ZINC), new int[]{1, 1}, lockedBronzeSplit(), new String[]{"copper", "zinc"}, new int[]{9, 9});
        assertEquals(TransferPlanner.Status.IGNORED, p.status());
        assertArrayEquals(new int[]{0, 0}, p.take());
    }

    @Test void missingListsIngredients() {
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, new RecipeLock(), new String[]{"copper", "dirt"}, new int[]{2, 64});
        assertEquals(TransferPlanner.Status.MISSING, p.status());
        assertEquals(List.of(0, 1), p.missing());
        assertArrayEquals(new int[]{0, 0}, p.take()); // nothing moved
    }

    @Test void countSumsAcrossInventoryStacks() {
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, new RecipeLock(), new String[]{"copper", "tin", "copper"}, new int[]{2, 1, 5});
        assertEquals(TransferPlanner.Status.OK, p.status());
        assertArrayEquals(new int[]{2, 1, 1}, p.take());
    }

    @Test void returnedItemsOverflowBecomesDrops() {
        // full inventory (2 slots: dirt 64, copper 60), returning 10 copper: 4 fit, 6 overflow -> dropped
        java.util.List<String> kinds = new java.util.ArrayList<>(List.of("dirt", "copper"));
        int[] cnt = {64, 60};
        assertEquals(6, TransferPlanner.returnInto(kinds, cnt, "copper", 10, Objects::equals, s -> 64));
        assertArrayEquals(new int[]{64, 64}, cnt);
        // empty slot available -> nothing overflows
        java.util.List<String> k2 = new java.util.ArrayList<>(java.util.Arrays.asList("dirt", null));
        int[] c2 = {64, 0};
        assertEquals(0, TransferPlanner.returnInto(k2, c2, "tin", 5, Objects::equals, s -> 64));
        assertEquals("tin", k2.get(1)); assertEquals(5, c2[1]);
    }

    @Test void transferSucceedsEvenWhenInventoryFull() {
        // pool = full inventory (no empty slot) + furnace slots holding zinc 5 and junk 7; recipe copper+tin
        // the plan still succeeds; junk/zinc leftovers would be returned and overflow -> drops (handled by returnInto)
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, new RecipeLock(),
                new String[]{"copper", "tin", "zinc", "junk"}, new int[]{64, 64, 5, 7});
        assertEquals(TransferPlanner.Status.OK, p.status());
        assertArrayEquals(new int[]{3, 1, 0, 0}, p.take());
        java.util.List<String> inv = new java.util.ArrayList<>(List.of("copper", "tin"));
        int[] ic = {61, 63};
        assertEquals(5, TransferPlanner.returnInto(inv, ic, "zinc", 5, Objects::equals, s -> 64));
        assertEquals(7, TransferPlanner.returnInto(inv, ic, "junk", 7, Objects::equals, s -> 64));
    }

    @Test void existingFurnaceItemsCountAsPool() {
        // AlloyTransfer returns furnace contents into the pool first; e.g. 3 copper that were in the furnace are taken back
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, new RecipeLock(), new String[]{"tin", "copper"}, new int[]{1, 3});
        assertEquals(TransferPlanner.Status.OK, p.status());
    }

    @Test void tagUsesFirstMatchingKindWithEnough() {
        var p = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, new RecipeLock(),
                new String[]{"othermod_copper", "copper", "tin"}, new int[]{3, 3, 1});
        assertEquals(0, p.slotSource()[0]); // first matching (othermod_copper)
        var p2 = plan("x:bronze", List.of(COPPER, TIN), new int[]{3, 1}, new RecipeLock(),
                new String[]{"othermod_copper", "copper", "tin"}, new int[]{1, 3, 1});
        assertEquals(1, p2.slotSource()[0]); // first kind lacks the amount -> next kind (no mixing kinds in one slot)
    }

    @Test void slotOverflowReported() {
        var p = TransferPlanner.plan("x:y", List.of(COPPER), new int[]{20}, new RecipeLock(), List.of("copper"), new int[]{64}, Objects::equals, s -> 16);
        assertEquals(TransferPlanner.Status.SLOT_OVERFLOW, p.status());
    }
}

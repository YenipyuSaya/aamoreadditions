package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** Efficient-Smelting rules with strings as items ("" = empty slot). */
class EfficientMatcherTest {
    record R(String id, String in, int n, int out, EfficientKind kind) {}

    /** The generated recipe set for one or more metals. */
    static List<R> recipes(String... metals) {
        List<R> l = new ArrayList<>();
        for (String m : metals) {
            l.add(new R(m + "/raw_x2", "raw_" + m, 2, 3, EfficientKind.RAW_ORE));
            l.add(new R(m + "/raw_x3", "raw_" + m, 3, 5, EfficientKind.RAW_ORE));
            l.add(new R(m + "/ore", "ore_" + m, 1, 2, EfficientKind.ORE));
            l.add(new R(m + "/raw_block", "rawblock_" + m, 1, 16, EfficientKind.RAW_BLOCK));
            l.add(new R(m + "/dust", "dust_" + m, 1, 1, EfficientKind.DUST));
        }
        return l;
    }

    static EfficientMatcher.Candidate<R> run(List<R> rs, String a, String b, String c) {
        List<String> items = List.of(a, b, c);
        boolean[] occ = {!a.isEmpty(), !b.isEmpty(), !c.isEmpty()};
        List<EfficientMatcher.Candidate<R>> cands = new ArrayList<>();
        for (R r : rs) {
            List<Predicate<String>> p = new ArrayList<>();
            for (int i = 0; i < r.n; i++) p.add(s -> s.equals(r.in));
            int[] as = EfficientMatcher.assign(p, items, occ);
            if (as != null) cands.add(new EfficientMatcher.Candidate<>(r, r.id, r.kind.priority, as));
        }
        return EfficientMatcher.pick(cands);
    }

    static final List<R> ALL = recipes("iron", "copper");

    @Test void rawCountsOccupiedSlots() {
        assertNull(run(ALL, "raw_iron", "", ""));                              // 1 raw: nothing
        var two = run(ALL, "raw_iron", "", "raw_iron");
        assertEquals("iron/raw_x2", two.id()); assertArrayEquals(new int[]{1, 0, 1}, two.plan());
        assertEquals("iron/raw_x3", run(ALL, "raw_iron", "raw_iron", "raw_iron").id()); // 3 slots -> 5 only, never 2->3
    }

    @Test void stackSizeIsIrrelevantOneItemPerSlot() {
        // the matcher never looks at counts: 64 raw in one slot is still "one slot of raw"
        var c = run(ALL, "raw_iron", "raw_iron", "");
        for (int[] row : c.take()) assertEquals(1, Arrays.stream(row).sum());
    }

    @Test void mixedSlotsOnlyTheMatchingGroupRuns() {
        assertEquals("iron/raw_x2", run(ALL, "raw_iron", "raw_copper", "raw_iron").id());
        assertNull(run(ALL, "raw_iron", "raw_copper", ""));          // no group complete -> idle
        assertNull(run(ALL, "raw_iron", "stick", ""));
        var c = run(ALL, "stick", "raw_copper", "raw_copper");       // foreign item ignored
        assertEquals("copper/raw_x2", c.id()); assertArrayEquals(new int[]{0, 1, 1}, c.plan());
    }

    @Test void priorityDustRawBlockOreRaw() {
        assertEquals("copper/dust", run(ALL, "raw_iron", "raw_iron", "dust_copper").id());
        assertEquals("iron/raw_block", run(ALL, "ore_iron", "rawblock_iron", "raw_iron").id());
        assertEquals("iron/ore", run(ALL, "raw_iron", "raw_iron", "ore_iron").id());
        assertEquals("iron/dust", run(ALL, "rawblock_copper", "dust_iron", "ore_copper").id());
    }

    @Test void singleInputsUseFirstSlotAndOneItem() {
        var c = run(ALL, "", "ore_iron", "ore_iron");
        assertEquals("iron/ore", c.id()); assertArrayEquals(new int[]{0, 1, 0}, c.plan());
        c = run(ALL, "ore_copper", "ore_iron", "");                  // same kind: earliest slot wins
        assertEquals("copper/ore", c.id()); assertArrayEquals(new int[]{1, 0, 0}, c.plan());
        c = run(ALL, "dust_iron", "dust_iron", "dust_iron");
        assertArrayEquals(new int[]{1, 0, 0}, c.plan());
    }

    @Test void kindFromTags() {
        assertEquals(EfficientKind.DUST, EfficientKind.ofTags(List.of("c:dusts/iron")));
        assertEquals(EfficientKind.RAW_BLOCK, EfficientKind.ofTags(List.of("c:storage_blocks/raw_iron", "c:storage_blocks")));
        assertEquals(EfficientKind.ORE, EfficientKind.ofTags(List.of("c:ores/iron", "c:ores")));
        assertEquals(EfficientKind.RAW_ORE, EfficientKind.ofTags(List.of("c:raw_materials/iron")));
        assertEquals(EfficientKind.OTHER, EfficientKind.ofTags(List.of("c:ingots/iron", "c:storage_blocks/iron")));
        assertEquals(8, EfficientKind.RAW_BLOCK.timeFactor);
        for (var k : EfficientKind.values()) if (k != EfficientKind.RAW_BLOCK) assertEquals(1, k.timeFactor);
        assertTrue(EfficientKind.DUST.priority > EfficientKind.RAW_BLOCK.priority
                && EfficientKind.RAW_BLOCK.priority > EfficientKind.ORE.priority
                && EfficientKind.ORE.priority > EfficientKind.RAW_ORE.priority
                && EfficientKind.RAW_ORE.priority > EfficientKind.OTHER.priority);
    }

    @Test void efficientModeRestartsOnRecipeChangeOnly() {
        assertTrue(EfficientMatcher.restartOnChange(true, "a:iron/raw_block", "a:iron/dust"));
        assertFalse(EfficientMatcher.restartOnChange(true, "a:iron/ore", "a:iron/ore"));
        assertFalse(EfficientMatcher.restartOnChange(false, "a:x", "a:y"));   // Alloy mode keeps progress
        assertFalse(EfficientMatcher.restartOnChange(true, null, "a:y"));     // after a reload: adopt
        assertFalse(EfficientMatcher.restartOnChange(true, "a:x", null));     // idle resets anyway elsewhere
    }
}

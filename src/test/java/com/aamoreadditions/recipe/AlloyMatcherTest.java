package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class AlloyMatcherTest {
    // "tags": copper tag matches copper and modcopper
    static Predicate<String> tag(String... ids) { return s -> s != null && Arrays.asList(ids).contains(s); }
    static final Predicate<String> COPPER = tag("copper", "othermod_copper"), ZINC = tag("zinc");

    static int[] m(List<Predicate<String>> ings, int[] need, String a, int ca, String b, int cb, String c, int cc) {
        return AlloyMatcher.match(ings, need, Arrays.asList(a, b, c), new int[]{ca, cb, cc});
    }

    @Test void anySlotOrder() {
        var ings = List.of(COPPER, ZINC); int[] need = {1, 1};
        assertArrayEquals(new int[]{1, 1, 0}, m(ings, need, "copper", 5, "zinc", 5, null, 0));
        assertArrayEquals(new int[]{1, 1, 0}, m(ings, need, "zinc", 5, "copper", 5, null, 0));
        assertArrayEquals(new int[]{0, 1, 1}, m(ings, need, null, 0, "zinc", 1, "copper", 1)); // extra slot used
        assertArrayEquals(new int[]{1, 0, 1}, m(ings, need, "copper", 1, null, 0, "zinc", 1));
    }

    @Test void tagMatchesOtherModItem() {
        assertNotNull(m(List.of(COPPER, ZINC), new int[]{1, 1}, "othermod_copper", 1, "zinc", 1, null, 0));
    }

    @Test void countSplitAcrossSlots() {
        // 3 copper + 1 zinc: copper 2 in slot 1 and 1 in extra
        assertArrayEquals(new int[]{2, 1, 1}, m(List.of(COPPER, ZINC), new int[]{3, 1}, "copper", 2, "zinc", 1, "copper", 1));
    }

    @Test void sameItemInTwoSlots() {
        var r = m(List.of(COPPER), new int[]{2}, "copper", 1, "copper", 1, null, 0);
        assertArrayEquals(new int[]{1, 1, 0}, r);
        // one slot feeding two ingredients of the same item
        assertArrayEquals(new int[]{2, 0, 0}, m(List.of(COPPER, COPPER), new int[]{1, 1}, "copper", 2, null, 0, null, 0));
    }

    @Test void notEnoughOrMissing() {
        assertNull(m(List.of(COPPER, ZINC), new int[]{1, 1}, "copper", 5, null, 0, null, 0));
        assertNull(m(List.of(COPPER, ZINC), new int[]{3, 1}, "copper", 2, "zinc", 1, null, 0));
        assertNull(m(List.of(COPPER, ZINC), new int[]{1, 1}, "copper", 1, "zinc", 1, "dirt", 1)); // foreign item blocks
    }

    @Test void needsBacktracking() {
        // ingredient A accepts copper or zinc, B only zinc; slots: zinc 1, copper 1 -> A must take copper
        var a = tag("copper", "zinc");
        assertArrayEquals(new int[]{1, 1, 0}, m(List.of(a, ZINC), new int[]{1, 1}, "zinc", 1, "copper", 1, null, 0));
    }

    @Test void bronzeThreePlusOneAnySplit() {
        Predicate<String> tin = tag("tin"); var ings = List.of(COPPER, tin); int[] need = {3, 1};
        assertArrayEquals(new int[]{3, 1, 0}, m(ings, need, "copper", 3, "tin", 1, null, 0));
        assertArrayEquals(new int[]{1, 1, 2}, m(ings, need, "copper", 1, "tin", 1, "copper", 2)); // copper in input1 + extra
        assertArrayEquals(new int[]{1, 2, 1}, m(ings, need, "copper", 1, "copper", 2, "tin", 1)); // tin in extra
        assertArrayEquals(new int[]{1, 3, 0}, m(ings, need, "tin", 1, "copper", 64, null, 0));
        assertNull(m(ings, need, "copper", 1, "copper", 1, "tin", 1)); // only 2 copper
        assertNull(m(ings, need, "copper", 3, null, 0, null, 0));       // no tin
    }

    @Test void netheriteTwoPlusTwoAnySlots() {
        Predicate<String> scrap = tag("netherite_scrap"), gold = tag("gold_ingot", "othermod_gold");
        var ings = List.of(scrap, gold); int[] need = {2, 2};
        assertArrayEquals(new int[]{2, 2, 0}, m(ings, need, "netherite_scrap", 2, "gold_ingot", 2, null, 0));
        assertArrayEquals(new int[]{2, 2, 0}, m(ings, need, "gold_ingot", 5, "netherite_scrap", 2, null, 0));
        assertArrayEquals(new int[]{1, 2, 1}, m(ings, need, "netherite_scrap", 1, "othermod_gold", 2, "netherite_scrap", 1));
        assertNull(m(ings, need, "netherite_scrap", 1, "gold_ingot", 2, null, 0));
    }
}

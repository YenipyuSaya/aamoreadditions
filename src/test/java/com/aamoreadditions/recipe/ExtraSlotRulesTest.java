package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

class ExtraSlotRulesTest {
    static final List<Predicate<String>> BRONZE = List.of(s -> Set.of("copper", "othermod_copper").contains(s), "tin"::equals);
    static IntPredicate item(String it) { return i -> BRONZE.get(i).test(it); }
    static boolean auto(RecipeLock l, String it) { return ExtraSlotRules.canAutoInsert(l.allows(2, item(it))); }

    static RecipeLock record(String a, int ca, String b, int cb, String c, int cc) {
        RecipeLock l = new RecipeLock(); l.toggle();
        assertTrue(l.recordIfPending("x:bronze", AlloyMatcher.matchDetailed(BRONZE, new int[]{3, 1}, Arrays.asList(a, b, c), new int[]{ca, cb, cc})));
        return l;
    }

    @Test void unlockedAndPendingAcceptIngredients() {
        RecipeLock l = new RecipeLock();
        assertTrue(auto(l, "tin")); assertTrue(auto(l, "dirt"));
        l.toggle(); assertTrue(auto(l, "copper")); assertTrue(auto(l, "dirt"));
    }
    @Test void lockedExtraAcceptsSameTagRejectsOthers() {
        RecipeLock l = record("copper", 1, "tin", 1, "copper", 2);
        assertTrue(auto(l, "copper")); assertTrue(auto(l, "othermod_copper")); assertFalse(auto(l, "tin")); assertFalse(auto(l, "dirt"));
    }
    @Test void lockedForbiddenExtraRejectsAll() {
        RecipeLock l = record("copper", 3, "tin", 1, null, 0);
        assertFalse(auto(l, "copper")); assertFalse(auto(l, "tin")); assertFalse(auto(l, "dirt"));
    }
    @Test void neverExtractable() { assertFalse(ExtraSlotRules.canAutoExtract()); }
}

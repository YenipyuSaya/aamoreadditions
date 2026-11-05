package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class RecipeLockTest {
    // "recipe": ingredient 0 = copper tag {copper, othermod_copper}, ingredient 1 = tin
    static final List<Predicate<String>> BRONZE = List.of(s -> s != null && Set.of("copper", "othermod_copper").contains(s), s -> "tin".equals(s));

    static IntPredicate item(String it) { return i -> BRONZE.get(i).test(it); }

    static RecipeLock recordBronze(String a, int ca, String b, int cb, String c, int cc) {
        int[][] take = AlloyMatcher.matchDetailed(BRONZE, new int[]{3, 1}, Arrays.asList(a, b, c), new int[]{ca, cb, cc});
        assertNotNull(take);
        RecipeLock l = new RecipeLock();
        l.toggle();
        assertTrue(l.locked()); assertFalse(l.recorded());
        assertTrue(l.allows(2, item("dirt"))); // pending: no restriction
        assertTrue(l.recordIfPending("mekanism_compat:bronze", take));
        return l;
    }

    @Test void mappingFollowsActualSlots() {
        // copper split over input 1 and extra, tin in input 2
        RecipeLock l = recordBronze("copper", 1, "tin", 1, "copper", 2);
        assertEquals(0, l.ingredientFor(0));
        assertEquals(1, l.ingredientFor(1));
        assertEquals(0, l.ingredientFor(2));
    }

    @Test void unusedSlotForbidden() {
        RecipeLock l = recordBronze("copper", 3, "tin", 1, null, 0);
        assertEquals(RecipeLock.FORBIDDEN, l.ingredientFor(2));
        assertFalse(l.allows(2, item("copper")));
        assertFalse(l.allows(2, item("tin")));
    }

    @Test void insertChecksIncludingTag() {
        RecipeLock l = recordBronze("copper", 3, "tin", 1, null, 0);
        assertTrue(l.allows(0, item("copper")));
        assertTrue(l.allows(0, item("othermod_copper"))); // tag member
        assertFalse(l.allows(0, item("tin")));
        assertTrue(l.allows(1, item("tin")));
        assertFalse(l.allows(1, item("copper")));
        l.toggle(); // unlock
        assertFalse(l.locked());
        assertTrue(l.allows(0, item("tin")));
        assertTrue(l.allows(2, item("dirt")));
    }

    @Test void recordsOnlyOnce() {
        RecipeLock l = recordBronze("copper", 3, "tin", 1, null, 0);
        assertFalse(l.recordIfPending("other", new int[][]{{0, 0, 1}}));
        assertEquals("mekanism_compat:bronze", l.recipeId());
    }

    @Test void persistenceRoundTrip() {
        RecipeLock l = recordBronze("copper", 1, "tin", 1, "copper", 2);
        RecipeLock r = new RecipeLock();
        r.decode(l.locked(), l.encodedId(), l.encodedMap());
        assertTrue(r.recorded());
        assertEquals(l.recipeId(), r.recipeId());
        assertArrayEquals(l.encodedMap(), r.encodedMap());
        // pending survives too
        RecipeLock p = new RecipeLock(); p.toggle();
        RecipeLock p2 = new RecipeLock(); p2.decode(p.locked(), p.encodedId(), p.encodedMap());
        assertTrue(p2.locked()); assertFalse(p2.recorded());
        // unlocked / broken data -> unlocked
        RecipeLock u = new RecipeLock(); u.decode(true, "x:y", new int[]{0});
        assertFalse(u.locked());
        RecipeLock u2 = new RecipeLock(); u2.decode(false, "x:y", new int[]{0, 1, -1});
        assertFalse(u2.locked());
    }

    @Test void missingRecipeUnlocks() {
        RecipeLock l = recordBronze("copper", 3, "tin", 1, null, 0);
        assertFalse(l.validate(id -> true));
        assertTrue(l.recorded());
        assertTrue(l.validate(id -> false));
        assertFalse(l.locked());
        assertTrue(l.allows(2, item("dirt")));
    }
}

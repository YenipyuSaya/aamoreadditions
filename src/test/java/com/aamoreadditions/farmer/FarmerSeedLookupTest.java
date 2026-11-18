package com.aamoreadditions.farmer;

import com.aamoreadditions.farmer.FarmerSeedLookup.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class FarmerSeedLookupTest {
    static final Function<String, Node> NO_TABLES = id -> null;

    /** Kinds from two sets: seed-tag members and items that place the crop. */
    static Function<String, Kind> kinds(Set<String> seeds, Set<String> places) {
        return id -> new Kind(seeds.contains(id), places.contains(id));
    }

    // ---------- static loot-table walk ----------

    @Test void everyEntryCountsRegardlessOfChanceOrCondition() {
        // vanilla wheat: alternatives[ wheat (if mature), seeds ] + bonus seeds pool; plus a 1-in-10^7 drop:
        // chances/conditions are not part of the node model at all, so they cannot hide anything.
        Node wheat = new Group(List.of(
                new Group(List.of(new Group(List.of(new Item("minecraft:wheat"), new Item("minecraft:wheat_seeds"))))),
                new Group(List.of(new Item("minecraft:wheat_seeds"))),
                new Group(List.of(new Item("mod:one_in_ten_million_seed")))));
        assertEquals(Set.of("minecraft:wheat", "minecraft:wheat_seeds", "mod:one_in_ten_million_seed"), FarmerSeedLookup.items(wheat, NO_TABLES));
    }

    @Test void nestedGroupsTagsAndNonItemEntries() {
        Node t = new Group(List.of(new Group(List.of(new Group(List.of(new Items(List.of("a:x", "a:y")), new Other()))))));
        assertEquals(Set.of("a:x", "a:y"), FarmerSeedLookup.items(t, NO_TABLES));
        assertTrue(FarmerSeedLookup.items(new Group(List.of()), NO_TABLES).isEmpty());
    }

    @Test void referencedTablesAreFollowedAndCyclesCut() {
        Map<String, Node> tables = new HashMap<>();
        tables.put("crop", new Group(List.of(new Item("a:produce"), new Ref("shared"))));
        tables.put("shared", new Group(List.of(new Item("a:seed"), new Ref("crop"), new Ref("shared"), new Ref("missing"))));
        assertEquals(Set.of("a:produce", "a:seed"), FarmerSeedLookup.items(new Ref("crop"), tables::get));
    }

    // ---------- choice (seed = c:seeds or minecraft:villager_plantable_seeds) ----------

    @Test void seedPlacingTheCropWins() {
        var drops = List.of("minecraft:wheat", "minecraft:wheat_seeds", "mod:a_other_seed");
        assertEquals("minecraft:wheat_seeds", FarmerSeedLookup.pick(drops,
                kinds(Set.of("minecraft:wheat_seeds", "mod:a_other_seed"), Set.of("minecraft:wheat_seeds"))));
    }

    @Test void villagerPlantableProduceIsASeed() {
        // vanilla potato: not c:seeds but in villager_plantable_seeds; poisonous potato is neither
        assertEquals("minecraft:potato", FarmerSeedLookup.pick(List.of("minecraft:poisonous_potato", "minecraft:potato"),
                kinds(Set.of("minecraft:potato"), Set.of("minecraft:potato"))));
    }

    @Test void otherwiseLowestSeedIdAndPlacingNonSeedsDoNotCount() {
        assertEquals("mod:a_seed", FarmerSeedLookup.pick(List.of("mod:b_seed", "mod:produce", "mod:a_seed"),
                kinds(Set.of("mod:a_seed", "mod:b_seed"), Set.of())));
        assertEquals("mod:seed", FarmerSeedLookup.pick(List.of("mod:produce", "mod:seed"),
                kinds(Set.of("mod:seed"), Set.of("mod:produce"))), "an untagged item that places the crop is not a seed");
        assertEquals("a:p1", FarmerSeedLookup.pick(List.of("a:p2", "a:p1"), kinds(Set.of("a:p1", "a:p2"), Set.of("a:p1", "a:p2"))));
    }

    @Test void noSeedCandidateMeansNoSeed() {
        assertNull(FarmerSeedLookup.pick(List.of("minecraft:stick"), kinds(Set.of(), Set.of())));
        assertNull(FarmerSeedLookup.pick(List.of("mod:produce"), kinds(Set.of(), Set.of("mod:produce"))), "no direct-placing fallback");
        assertNull(FarmerSeedLookup.pick(List.of(), kinds(Set.of(), Set.of())));
    }

    @Test void rareDropIsFoundAndChosen() {
        Node t = new Group(List.of(new Item("mod:produce"), new Group(List.of(new Item("mod:rare_seed"))))); // e.g. random_chance 1e-7
        assertEquals("mod:rare_seed", FarmerSeedLookup.pick(FarmerSeedLookup.items(t, NO_TABLES), kinds(Set.of("mod:rare_seed"), Set.of("mod:rare_seed"))));
    }

    @Test void cachedPerBlockAndRecomputedOnlyAfterReload() {
        var cache = new FarmerSeedLookup<String>();
        var calls = new AtomicInteger();
        Object gen1 = new Object(), gen2 = new Object();
        for (int i = 0; i < 5; i++) assertEquals("seed", cache.get("wheat", gen1, () -> { calls.incrementAndGet(); return "seed"; }));
        assertEquals(1, calls.get());
        assertNull(cache.get("odd", gen1, () -> { calls.incrementAndGet(); return null; }));
        assertNull(cache.get("odd", gen1, () -> { calls.incrementAndGet(); return "x"; }), "no-seed results are cached too");
        assertEquals(2, calls.get());
        assertEquals("new", cache.get("wheat", gen2, () -> { calls.incrementAndGet(); return "new"; }));
        assertEquals(3, calls.get());
        assertEquals(1, cache.size());
        // generations compare by value: an equal (tables, candidates) key keeps the cache, a changed list clears it
        Object tables = new Object();
        cache.get("wheat", List.of(tables, Set.of("c:seeds")), () -> { calls.incrementAndGet(); return "s"; });
        cache.get("wheat", List.of(tables, Set.of("c:seeds")), () -> { calls.incrementAndGet(); return "s"; });
        assertEquals(4, calls.get());
        cache.get("wheat", List.of(tables, Set.of("c:seeds", "x:y")), () -> { calls.incrementAndGet(); return "s"; });
        assertEquals(5, calls.get());
    }
}

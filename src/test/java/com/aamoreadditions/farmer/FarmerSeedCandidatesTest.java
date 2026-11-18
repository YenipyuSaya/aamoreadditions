package com.aamoreadditions.farmer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FarmerSeedCandidatesTest {
    @Test void defaultsAreTheThreeTags() {
        assertEquals(List.of("#c:seeds", "#minecraft:villager_plantable_seeds", "#aamoreadditions:farmer_memory_seeds"), FarmerSeedCandidates.DEFAULTS);
        var c = FarmerSeedCandidates.parse(FarmerSeedCandidates.DEFAULTS);
        assertEquals(Set.of("c:seeds", "minecraft:villager_plantable_seeds", "aamoreadditions:farmer_memory_seeds"), c.tags());
        assertTrue(c.items().isEmpty()); assertTrue(c.invalid().isEmpty());
    }

    @Test void itemsTagsAndInvalidEntries() {
        var c = FarmerSeedCandidates.parse(Arrays.asList("minecraft:wheat_seeds", " #c:crops/rice ", "#", "", "no_namespace", "Bad:Caps", "#c:", null, "mod:a/b"));
        assertEquals(Set.of("minecraft:wheat_seeds", "mod:a/b"), c.items());
        assertEquals(Set.of("c:crops/rice"), c.tags());
        assertEquals(List.of("#", "", "no_namespace", "Bad:Caps", "#c:", "null"), c.invalid());
    }

    @Test void matchingByItemOrTag() {
        var c = FarmerSeedCandidates.parse(List.of("minecraft:sugar_cane", "#c:seeds"));
        assertTrue(c.matches("minecraft:sugar_cane", t -> false));
        assertTrue(c.matches("minecraft:wheat_seeds", "c:seeds"::equals));
        assertFalse(c.matches("minecraft:potato", t -> false), "removing a default really removes it");
        assertFalse(FarmerSeedCandidates.parse(List.of()).matches("minecraft:wheat_seeds", t -> true), "empty list = no seeds at all");
    }

    @Test void equalListsParseEqualSoTheSeedCacheSurvivesUnchangedReloads() {
        assertEquals(FarmerSeedCandidates.parse(List.of("#c:seeds", "a:b")), FarmerSeedCandidates.parse(List.of("a:b", "#c:seeds")));
        assertNotEquals(FarmerSeedCandidates.parse(List.of("#c:seeds")), FarmerSeedCandidates.parse(List.of("#c:seeds", "a:b")));
    }
}

package com.aamoreadditions.farmer;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

/** Every memory-mode rule on the pure {@link FarmerMemory}. */
class FarmerMemoryTest {
    static final String WHEAT = "minecraft:wheat_seeds", CARROT = "minecraft:carrot";
    static final Predicate<String> FITS = s -> true, NOT_FITS = s -> false;

    static FarmerMemory memory() { var m = new FarmerMemory(); m.setMemory(true); return m; }

    @Test void outOfOrderModeRecordsNothingTillsNothingAndPlantsAnything() {
        var m = new FarmerMemory();
        m.observe(1, WHEAT, false); m.observe(2, null, true);
        assertTrue(m.snapshot().isEmpty());
        assertFalse(m.shouldTill(1, s -> true));
        assertTrue(m.allowsPlant(1, CARROT, NOT_FITS));
    }

    @Test void observeIsTheSingleRecordPath() {
        var m = memory();
        m.observe(1, WHEAT, false);
        assertEquals(WHEAT, m.record(1), "crop seen (before harvest) or planted");
        m.observe(1, CARROT, false);
        assertEquals(CARROT, m.record(1), "a different crop harvested/planted later updates it");
        m.observe(1, null, true);
        assertEquals(CARROT, m.record(1), "bare farmland never erases a seed");
        m.observe(2, null, true);
        assertEquals(FarmerMemory.EMPTY, m.record(2), "first sight of bare farmland (or freshly tilled)");
        m.observe(2, WHEAT, false);
        assertEquals(WHEAT, m.record(2), "a crop appearing later overwrites EMPTY");
        m.observe(3, null, false); m.observe(3, "", false);
        assertNull(m.record(3), "grass, dirt, fruit blocks... are not recorded");
    }

    @Test void plantingGateNeedsTheRecordedSeedAndAFittingGround() {
        var m = memory();
        m.observe(1, WHEAT, false); m.observe(2, null, true);
        assertTrue(m.allowsPlant(1, WHEAT, FITS));
        assertFalse(m.allowsPlant(1, CARROT, FITS), "only the recorded seed");
        assertFalse(m.allowsPlant(1, null, FITS));
        assertFalse(m.allowsPlant(1, WHEAT, NOT_FITS), "ground not tilled yet: till first, never via AA's defaultPlant");
        assertFalse(m.allowsPlant(2, WHEAT, FITS), "EMPTY is never planted");
        assertFalse(m.allowsPlant(3, WHEAT, FITS), "unrecorded is never planted");
    }

    @Test void tillingRules() {
        var m = memory();
        Predicate<String> unused = s -> fail("tilling need is only consulted for seed records");
        assertTrue(m.shouldTill(9, unused), "unrecorded (grass, dirt, coarse/rooted dirt step)");
        m.observe(9, null, true);
        assertTrue(m.shouldTill(9, unused), "EMPTY farmland that turned back into dirt");
        m.observe(1, WHEAT, false);
        assertFalse(m.shouldTill(1, s -> false), "recorded crop needs no tilling (intact farmland; nether wart, sugar cane, cactus)");
        assertTrue(m.shouldTill(1, s -> true), "recorded farmland crop whose farmland turned back into dirt");
        m.observe(1, null, true);
        assertEquals(WHEAT, m.record(1), "tilling keeps the seed record, so it is replanted afterwards");
    }

    @Test void modeSwitching() {
        var m = new FarmerMemory();
        assertTrue(m.toggle());
        m.observe(1, WHEAT, false); m.observe(2, null, true);
        assertFalse(m.toggle());
        assertTrue(m.snapshot().isEmpty(), "back to out-of-order deletes all records");
        m.setMemory(true);
        assertNull(m.record(1));
        m.observe(1, WHEAT, false);
        m.setMemory(true);
        assertEquals(WHEAT, m.record(1), "re-setting the same mode (client sync) keeps records");
    }

    @Test void saveLoadRoundTripAndMalformedInput() {
        var m = memory();
        m.observe(10, WHEAT, false); m.observe(-20, null, true); m.observe(30, CARROT, false);
        var snap = m.snapshot();
        long[] pos = snap.keySet().stream().mapToLong(Long::longValue).toArray();
        var n = new FarmerMemory();
        n.load(true, pos, Arrays.stream(pos).mapToObj(snap::get).toList());
        assertTrue(n.memory());
        assertEquals(snap, n.snapshot());
        n.load(false, new long[]{1}, List.of(WHEAT));
        assertTrue(n.snapshot().isEmpty());
        n.load(true, new long[]{1, 2}, List.of(WHEAT));
        assertEquals(1, n.snapshot().size());
        n.load(true, null, null);
        assertTrue(n.memory()); assertTrue(n.snapshot().isEmpty());
    }

    @Test void netherWartSugarCaneAndCactusAreRecordedButTheirGroundIsNeverTilled() {
        var m = memory();
        for (String seed : new String[]{"minecraft:nether_wart", "minecraft:sugar_cane", "minecraft:cactus"}) {
            long pos = seed.hashCode();
            m.observe(pos, seed, false);                 // base block seen at the scanned layer
            assertEquals(seed, m.record(pos));
            assertFalse(m.shouldTill(pos, s -> false), "needsTilling is false for non-farmland plants");
            assertTrue(m.allowsPlant(pos, seed, FITS), "replanted at the base when it can stand there");
            assertFalse(m.allowsPlant(pos, seed, NOT_FITS), "e.g. no water next to the cane, a block next to the cactus");
        }
    }

    @Test void areaChangeDropsOnlyRecordsOutsideTheScannedSquare() {
        var m = memory();
        m.observe(1, WHEAT, false); m.observe(2, null, true); m.observe(3, CARROT, false);
        m.retain(p -> p != 2);
        assertEquals(java.util.Map.of(1L, WHEAT, 3L, CARROT), m.snapshot());
        m.observe(1, CARROT, false);
        assertEquals(CARROT, m.record(1), "records left in the area are still overwritten as before");
        // 5x5 area (radius 2) around center (10, 64, 10)
        assertTrue(FarmerMemory.inSquare(10, 64, 10, 2, 8, 64, 12));
        assertTrue(FarmerMemory.inSquare(10, 64, 10, 2, 12, 64, 8));
        assertFalse(FarmerMemory.inSquare(10, 64, 10, 2, 13, 64, 10), "range shrank");
        assertFalse(FarmerMemory.inSquare(10, 64, 10, 2, 10, 64, 7), "facing turned");
        assertFalse(FarmerMemory.inSquare(10, 64, 10, 2, 10, 65, 10), "other layer");
    }
}

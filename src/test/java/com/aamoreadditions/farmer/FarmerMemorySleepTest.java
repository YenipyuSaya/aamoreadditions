package com.aamoreadditions.farmer;

import com.aamoreadditions.effect.FarmerScanState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Memory mode on the real smart-sleep {@link FarmerScanState}, via a 3x3 field driven in the mixin's order:
 * beginCell (observe, then at most one hoe step = work) -> planting gate (planting = work) -> scan accounting.
 */
class FarmerMemorySleepTest {
    static final String WHEAT = "minecraft:wheat_seeds";
    /** Ground: 2 coarse/rooted dirt, 1 grass/dirt, 0 farmland, -1 not hoe-tillable (sand...). */
    static final class Field {
        final FarmerScanState scan = new FarmerScanState();
        final FarmerMemory mem = new FarmerMemory();
        final int[] ground = new int[9];
        final String[] crop = new String[9];
        int seeds, hoeSteps, plantings;
        Field(boolean memory) { scan.configure(3, 0, 1, true); mem.setMemory(memory); }

        void cell(int c) {
            boolean work = false;
            if (crop[c] != null) mem.observe(c, crop[c], false);
            else {
                mem.observe(c, null, ground[c] == 0);
                if (ground[c] > 0 && mem.shouldTill(c, s -> ground[c] != 0)) {
                    ground[c]--; hoeSteps++; work = true;
                    mem.observe(c, null, ground[c] == 0);
                }
                if (seeds > 0 && mem.memory() && mem.allowsPlant(c, WHEAT, s -> ground[c] == 0)) {
                    seeds--; crop[c] = WHEAT; plantings++; work = true; mem.observe(c, WHEAT, false);
                }
            }
            if (work) scan.planted(c); else scan.observe(c, false, false, false, false);
        }
        void rounds(int n) { for (int r = 0; r < n; r++) for (int c = 0; c < 9; c++) cell(c); }
    }

    @Test void idleFieldSleepsAtTheSameCellInBothModes() {
        var a = new Field(false); var b = new Field(true);
        for (int c = 0; c < 9; c++) b.mem.observe(c, c % 2 == 0 ? WHEAT : null, c % 2 == 1); // seedless records + EMPTY
        for (int c = 0; c < 9; c++) { a.cell(c); b.cell(c); assertEquals(a.scan.sleeping(), b.scan.sleeping(), "cell " + c); }
        assertTrue(b.scan.sleeping());
        b.rounds(3);
        assertTrue(b.scan.sleeping(), "EMPTY / seedless cells never wake it");
    }

    @Test void matureCropKeepsItAwakeAndSwitchingToMemoryWakesOnce() {
        var f = new Field(false);
        f.rounds(1);
        assertTrue(f.scan.sleeping());
        assertTrue(f.mem.toggle()); f.scan.reset(); // FarmerMemoryRuntime.toggle -> resetFarmerScan
        assertFalse(f.scan.sleeping());
        for (int c = 0; c < 9; c++) {
            if (c == 3) f.scan.observe(c, true, false, true, false); else f.cell(c); // one harvest that fits
        }
        assertFalse(f.scan.sleeping());
        f.rounds(1);
        assertTrue(f.scan.sleeping(), "then the normal sleep conditions apply");
    }

    @Test void unrecordedGroundIsHoedStepByStepUntilFarmlandThenStaysEmptyAndSleeps() {
        var f = new Field(true);
        f.ground[0] = 2; f.ground[1] = 2; f.ground[2] = 1; f.ground[3] = 1; f.ground[5] = -1; f.ground[6] = 1;
        f.seeds = 64;
        f.rounds(4);
        assertEquals(2 + 2 + 1 + 1 + 1, f.hoeSteps, "one hoe interaction per scan, coarse/rooted take two");
        assertEquals(350, FarmerMemoryRuntime.TILL_COST, "each step costs AA's tilling energy");
        assertEquals(0, f.plantings, "tilled farmland is never planted");
        for (int c : new int[]{0, 1, 2, 3, 4, 6, 7, 8}) assertEquals(FarmerMemory.EMPTY, f.mem.record(c));
        assertNull(f.mem.record(5), "non-tillable ground is ignored");
        assertTrue(f.scan.sleeping());
    }

    @Test void recordedDecayedGroundIsTilledFirstThenReplantedOrJustTilledWithoutSeeds() {
        var f = new Field(true);
        for (int c = 0; c < 9; c++) f.mem.observe(c, WHEAT, false);
        f.ground[0] = 2; f.ground[1] = 1; f.ground[2] = 2; f.ground[3] = 1;
        f.seeds = 1;
        f.rounds(4);
        assertEquals(6, f.hoeSteps, "tilled even when no seed is left");
        assertEquals(1, f.plantings);
        for (int c = 0; c < 9; c++) assertEquals(WHEAT, f.mem.record(c));
        assertTrue(f.scan.sleeping());
        f.seeds = 1; f.cell(1); f.cell(2);
        assertEquals(2, f.plantings, "seeds arriving later replant during slow scan");
        assertFalse(f.scan.sleeping(), "and that planting wakes it");
    }
}

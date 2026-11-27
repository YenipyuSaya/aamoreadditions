package com.aamoreadditions.digger;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiggerScanTest {
    @Test void phasesFollowAaCheckY() {
        assertEquals(DiggerScan.Phase.START, DiggerScan.phase(-65, -64));
        assertEquals(DiggerScan.Phase.DONE, DiggerScan.phase(-64, -64));
        assertEquals(DiggerScan.Phase.WORKING, DiggerScan.phase(10, -64));
    }

    @Test void cursorWalksXThenZThenOneLayerDown() {
        var c = DiggerScan.start(70, DiggerScan.relative(0, 0, 2, false));
        assertEquals(new DiggerScan.Cursor(-2, 69, -2), c);
        int steps = 0;
        while (c.y() == 69) { c = DiggerScan.next(c, DiggerScan.relative(0, 0, 2, false), 1, Integer.MIN_VALUE); steps++; }
        assertEquals(25, steps); // 5x5 layer
        assertEquals(new DiggerScan.Cursor(-2, 68, -2), c);
        assertEquals(new DiggerScan.Cursor(-2, 69, -1), DiggerScan.next(new DiggerScan.Cursor(2, 69, -2), DiggerScan.relative(0, 0, 2, false), 1, Integer.MIN_VALUE));
    }

    @Test void rangeChangeRestartsOnlyARunningScan() {
        assertTrue(DiggerScan.restartForArea(2, false, 0, 4, false, 0, DiggerScan.Phase.WORKING));
        assertFalse(DiggerScan.restartForArea(4, false, 0, 4, false, 0, DiggerScan.Phase.WORKING));
        assertFalse(DiggerScan.restartForArea(2, false, 0, 4, false, 0, DiggerScan.Phase.DONE));
        assertFalse(DiggerScan.restartForArea(2, false, 0, 4, false, 0, DiggerScan.Phase.START));
    }

    @Test void savedCursorResumesOnlyInsideTheSavedBox() {
        assertTrue(DiggerScan.resumable(new DiggerScan.Cursor(1, 40, -2), 70, -64, 0, 0, 2, false));
        assertTrue(DiggerScan.resumable(new DiggerScan.Cursor(0, -64, 0), 70, -64, 0, 0, 2, false)); // finished stays finished
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(3, 40, 0), 70, -64, 0, 0, 2, false));
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(0, 70, 0), 70, -64, 0, 0, 2, false));
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(0, -65, 0), 70, -64, 0, 0, 2, false));
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(0, 40, 0), 70, -64, 0, 0, 0, false)); // unknown range
    }

    @Test void fillersUseTheFirstNonEmptyPlaceableSlotAndSkipOthers() {
        List<String> slots = Arrays.asList("", "stick", "", "cobblestone", "dirt", "", "", "", "");
        java.util.function.Predicate<String> empty = String::isEmpty;
        java.util.function.Predicate<String> block = s -> !s.equals("stick");
        assertEquals(3, DiggerScan.firstPlaceable(slots, empty, block, 0));
        assertEquals(4, DiggerScan.firstPlaceable(slots, empty, block, 4)); // next candidate after a failed placement
        assertEquals(-1, DiggerScan.firstPlaceable(slots, empty, block, 5));
        assertEquals(-1, DiggerScan.firstPlaceable(Arrays.asList("stick", ""), empty, block, 0));
    }

    @Test void radiusAreaIsTheInclusiveSquare() {
        assertEquals(new DiggerScan.Area(-2, 2, -2, 2), DiggerScan.relative(99, -7, 2, false));
        assertEquals(new DiggerScan.Area(0, 0, 0, 0), DiggerScan.relative(99, -7, 0, false));
        assertEquals(new DiggerScan.Area(-16, 16, -16, 16), DiggerScan.relative(5, 5, 16, false));
    }

    @Test void chunkAreaCoversEveryIntersectingChunk() {
        // r=0: exactly the machine's chunk (16x16), including at edges and in negative chunks.
        assertEquals(new DiggerScan.Area(0, 15, 0, 15), DiggerScan.relative(0, 0, 0, true));
        assertEquals(new DiggerScan.Area(-15, 0, -15, 0), DiggerScan.relative(15, 15, 0, true));
        assertEquals(new DiggerScan.Area(0, 15, 0, 15), DiggerScan.relative(16, 16, 0, true));
        assertEquals(new DiggerScan.Area(-15, 0, -15, 0), DiggerScan.relative(-1, -1, 0, true));
        // r=2 at (0,0): square [-2,2] intersects chunks (-1,0) in both axes -> 32x32.
        assertEquals(new DiggerScan.Area(-16, 15, -16, 15), DiggerScan.relative(0, 0, 2, true));
        assertEquals(32 * 32, DiggerScan.relative(0, 0, 2, true).columns());
        // r=4 at (0,0): square [-4,4] still those two chunks.
        assertEquals(new DiggerScan.Area(-16, 15, -16, 15), DiggerScan.relative(0, 0, 4, true));
        // r=16 (three boosters) at (0,0): square [-16,16] also hits chunk 1.
        assertEquals(new DiggerScan.Area(-16, 31, -16, 31), DiggerScan.relative(0, 0, 16, true));
        // mid-chunk: the 5x5 stays inside one chunk, but the work area is the whole chunk.
        assertEquals(new DiggerScan.Area(-8, 7, -8, 7), DiggerScan.relative(8, 8, 2, true));
        // machine on the +X chunk edge: two chunks in X, one in Z.
        assertEquals(new DiggerScan.Area(-15, 16, -8, 7), DiggerScan.relative(15, 8, 2, true));
        // negative mid-chunk.
        assertEquals(new DiggerScan.Area(-8, 7, -8, 7), DiggerScan.relative(-8, -8, 2, true));
        // r=2 at (-1,-1): square [-3,1] hits chunks -1 and 0.
        assertEquals(new DiggerScan.Area(-15, 16, -15, 16), DiggerScan.relative(-1, -1, 2, true));
        // large range at a chunk edge.
        assertEquals(new DiggerScan.Area(-31, 16, -31, 16), DiggerScan.relative(15, 15, 16, true));
    }

    @Test void chunkAreaAlwaysContainsTheRadiusSquareAndIsChunkAligned() {
        int[] xs = {0, 1, 15, 16, -1, -16, 31, -17};
        int[] zs = {0, 7, 15, -1, -16};
        int[] rs = {0, 2, 4, 16};
        for (int ox : xs) for (int oz : zs) for (int r : rs) {
            var chunk = DiggerScan.relative(ox, oz, r, true);
            var radius = DiggerScan.relative(ox, oz, r, false);
            assertTrue(chunk.contains(radius.minX(), radius.minZ()), ox + "," + oz + " r=" + r);
            assertTrue(chunk.contains(radius.maxX(), radius.maxZ()), ox + "," + oz + " r=" + r);
            assertTrue(chunk.contains(0, 0), "origin in area");
            assertEquals(0, Math.floorMod(ox + chunk.minX(), 16));
            assertEquals(15, Math.floorMod(ox + chunk.maxX(), 16));
            assertEquals(0, Math.floorMod(oz + chunk.minZ(), 16));
            assertEquals(15, Math.floorMod(oz + chunk.maxZ(), 16));
        }
    }

    @Test void chunkScanWalksXThenZOverTheExpandedRectangle() {
        var area = DiggerScan.relative(0, 0, 0, true); // [0,15]^2
        var c = DiggerScan.start(70, area);
        assertEquals(new DiggerScan.Cursor(0, 69, 0), c);
        int steps = 0;
        while (c.y() == 69) { c = DiggerScan.next(c, area, 1, Integer.MIN_VALUE); steps++; }
        assertEquals(16 * 16, steps);
        assertEquals(new DiggerScan.Cursor(0, 68, 0), c);
        assertEquals(new DiggerScan.Cursor(0, 69, 1), DiggerScan.next(new DiggerScan.Cursor(15, 69, 0), area, 1, Integer.MIN_VALUE));
        // Asymmetric area (machine at +X edge, r=2): start is min corner, not (-r,-r).
        var edge = DiggerScan.relative(15, 8, 2, true);
        assertEquals(new DiggerScan.Cursor(-15, 69, -8), DiggerScan.start(70, edge));
        assertEquals(new DiggerScan.Cursor(-14, 69, -8), DiggerScan.next(DiggerScan.start(70, edge), edge, 1, Integer.MIN_VALUE));
    }

    @Test void modeChangeRestartsOnlyARunningScan() {
        assertTrue(DiggerScan.restartForArea(2, false, 0, 2, true, 0, DiggerScan.Phase.WORKING));
        assertFalse(DiggerScan.restartForArea(2, true, 0, 2, true, 0, DiggerScan.Phase.WORKING));
        assertFalse(DiggerScan.restartForArea(2, false, 0, 2, true, 0, DiggerScan.Phase.DONE));
        assertFalse(DiggerScan.restartForArea(2, false, 0, 2, true, 0, DiggerScan.Phase.START));
        assertTrue(DiggerScan.restartForArea(2, true, 0, 4, true, 0, DiggerScan.Phase.WORKING));
    }

    @Test void savedChunkCursorResumesInsideTheSavedChunkBox() {
        assertTrue(DiggerScan.resumable(new DiggerScan.Cursor(-16, 40, 15), 70, -64, 0, 0, 2, true));
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(-16, 40, 15), 70, -64, 0, 0, 2, false));
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(-17, 40, 0), 70, -64, 0, 0, 2, true));
        assertTrue(DiggerScan.resumable(new DiggerScan.Cursor(0, -64, 0), 70, -64, 0, 0, 2, true));
        // Mid-chunk origin: a radius-edge column is inside the chunk box.
        assertTrue(DiggerScan.resumable(new DiggerScan.Cursor(-8, 40, 7), 70, -64, 8, 8, 2, true));
        assertFalse(DiggerScan.resumable(new DiggerScan.Cursor(-8, 40, 7), 70, -64, 8, 8, 2, false));
    }

    @Test void layerAdvancesByTheVerticalSpanAndClampsToMinY() {
        var area = new DiggerScan.Area(-1, 1, -1, 1); // 3x3 columns
        // Mid-row: same Y, X advances (span is only applied after the last column of the band).
        assertEquals(new DiggerScan.Cursor(0, 69, -1), DiggerScan.next(new DiggerScan.Cursor(-1, 69, -1), area, 3, -64));
        // Last column of the band: Y drops by 3, cursor returns to min corner.
        assertEquals(new DiggerScan.Cursor(-1, 66, -1), DiggerScan.next(new DiggerScan.Cursor(1, 69, 1), area, 3, -64));
        // A short last band must become DONE (minY), not START (below minY).
        assertEquals(new DiggerScan.Cursor(-1, -64, -1), DiggerScan.next(new DiggerScan.Cursor(1, -63, 1), area, 3, -64));
        // Default next() is still one layer.
        assertEquals(new DiggerScan.Cursor(-1, 68, -1), DiggerScan.next(new DiggerScan.Cursor(1, 69, 1), area, 1, Integer.MIN_VALUE));
    }

    @Test void verticalSpanChangeRestartsOnlyARunningScan() {
        assertTrue(DiggerScan.restartForArea(2, false, 1, 2, false, 3, DiggerScan.Phase.WORKING));
        assertFalse(DiggerScan.restartForArea(2, false, 3, 2, false, 3, DiggerScan.Phase.WORKING));
        assertFalse(DiggerScan.restartForArea(2, false, 0, 2, false, 3, DiggerScan.Phase.WORKING)); // unknown span
        assertFalse(DiggerScan.restartForArea(2, false, 1, 2, false, 3, DiggerScan.Phase.DONE));
        assertFalse(DiggerScan.restartForArea(2, false, 1, 2, false, 3, DiggerScan.Phase.START));
    }
}

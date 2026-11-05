package com.aamoreadditions.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CasingCrossDisplayTest {
    @Test void absoluteCompassMappingUsesTheActualGuiSlotConstants() {
        assertEquals(CasingCrossData.SLOT_BASE, CasingCrossDisplayLayout.slotAt(0, 0, 0));
        assertEquals(CasingCrossData.SLOT_UP, CasingCrossDisplayLayout.slotAt(0, 0, -1));
        assertEquals(CasingCrossData.SLOT_RIGHT, CasingCrossDisplayLayout.slotAt(1, 0, 0));
        assertEquals(CasingCrossData.SLOT_DOWN, CasingCrossDisplayLayout.slotAt(0, 0, 1));
        assertEquals(CasingCrossData.SLOT_LEFT, CasingCrossDisplayLayout.slotAt(-1, 0, 0));
    }

    @Test void noCasingDiagonalOrOutputProjection() {
        assertEquals(-1, CasingCrossDisplayLayout.slotAt(0, -1, 0));
        assertEquals(-1, CasingCrossDisplayLayout.slotAt(0, 1, 0));
        assertEquals(-1, CasingCrossDisplayLayout.slotAt(1, 0, -1));
        assertEquals(-1, CasingCrossDisplayLayout.slotAt(0, 0, -2));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            assertNotEquals(CasingCrossData.SLOT_OUTPUT, CasingCrossDisplayLayout.slotAt(x, 0, z));
    }
}

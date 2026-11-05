package com.aamoreadditions.structure;

/** Pure compass mapping, kept independent of Minecraft types for regression tests. */
public final class CasingCrossDisplayLayout {
    private CasingCrossDisplayLayout() {}

    public static int slotAt(int x, int y, int z) {
        if (y != 0) return -1;
        if (x == 0 && z == 0) return CasingCrossData.SLOT_BASE;
        if (x == 0 && z == -1) return CasingCrossData.SLOT_UP;
        if (x == 1 && z == 0) return CasingCrossData.SLOT_RIGHT;
        if (x == 0 && z == 1) return CasingCrossData.SLOT_DOWN;
        if (x == -1 && z == 0) return CasingCrossData.SLOT_LEFT;
        return -1;
    }
}

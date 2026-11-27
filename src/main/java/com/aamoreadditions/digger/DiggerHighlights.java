package com.aamoreadditions.digger;

import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Client-side list of diggers seen through AA's SYNC tag; the renderer filters by highlight state. No client imports. */
public final class DiggerHighlights {
    private DiggerHighlights() {}

    private static final Set<TileEntityVerticalDigger> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());

    public static void track(TileEntityVerticalDigger tile) {
        TRACKED.add(tile);
    }

    public static java.util.List<TileEntityVerticalDigger> snapshot() {
        TRACKED.removeIf(t -> t.isRemoved());
        return java.util.List.copyOf(TRACKED);
    }
}

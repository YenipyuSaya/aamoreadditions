package com.aamoreadditions.digger;

import net.minecraft.world.item.ItemStack;

/** Per-tile runtime data of the digger (server side unless noted). */
public final class DiggerWork {
    /** Ticks since the last scanned position (not saved; AA used ticksElapsed % 5). */
    public int counter;
    /** Range the current scan started with (saved), 0 = unknown. */
    public int scanRange;
    /** Work-area mode the running scan was started with (SAVE_TILE only). */
    public boolean scanStrictChunk;
    /** Vertical span (1/3/5) the current scan started with (saved), 0 = unknown. */
    public int scanVerticalSpan;
    /** Current effective range (saved + synced, used by the highlight). */
    public int range = de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger.DEFAULT_RANGE;
    /** Last values pushed to clients (AA's private oldEnergy/oldCheck* equivalents). */
    public int oldEnergy = -1, oldX, oldY, oldZ, oldRange;
    /** Cursor read from disk; restored after AA's onLoad restart (null = nothing to resume). */
    public DiggerScan.Cursor savedCursor;
    /** One dummy AIOT per digger, re-created only when the enchantment key changes. */
    public ItemStack tool;
    public int toolKey = Integer.MIN_VALUE;
}

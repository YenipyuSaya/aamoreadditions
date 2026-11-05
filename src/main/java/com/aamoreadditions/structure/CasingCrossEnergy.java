package com.aamoreadditions.structure;

import com.aamoreadditions.AAMoreAdditions;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import net.minecraft.core.BlockPos;

/** Capacity of the structure's energy pool = AA Display Stand capacity * 4 (read from AA at runtime). */
public final class CasingCrossEnergy {
    /** Used only if AA's stand cannot be created; matches AA's current value (CustomEnergyStorage(80000, ...)). */
    private static final int FALLBACK_STAND_CAPACITY = 80_000;
    private static volatile int capacity = -1;

    private CasingCrossEnergy() {}

    public static int capacity() {
        int c = capacity;
        if (c < 0) {
            int stand;
            try {
                // throwaway stand, never placed in a level; follows AA if it changes the value
                stand = new TileEntityDisplayStand(BlockPos.ZERO, ActuallyBlocks.DISPLAY_STAND.get().defaultBlockState()).storage.getMaxEnergyStored();
            } catch (Throwable t) {
                AAMoreAdditions.LOGGER.warn("Could not read AA display stand capacity, using {}", FALLBACK_STAND_CAPACITY, t);
                stand = FALLBACK_STAND_CAPACITY;
            }
            c = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) stand * 4L));
            capacity = c;
        }
        return c;
    }
}

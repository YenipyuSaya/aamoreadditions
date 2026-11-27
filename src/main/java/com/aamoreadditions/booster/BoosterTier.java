package com.aamoreadditions.booster;

import com.aamoreadditions.block.ModBlocks;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import net.minecraft.world.level.block.Block;

/** Tiers of phantom-range boosters recognised in a vertical stack. */
public enum BoosterTier {
    /** Actually Additions Range Booster. */
    VANILLA,
    ADVANCED,
    TRANSFINITE,
    DIMENSIONAL;

    public boolean special() {
        return this == TRANSFINITE || this == DIMENSIONAL;
    }

    /** {@code null} if {@code block} is not a booster (breaks the stack). */
    public static BoosterTier of(Block block) {
        if (block == null) return null;
        if (block == ActuallyBlocks.PHANTOM_BOOSTER.get()) return VANILLA;
        if (block == ModBlocks.ADVANCED_PHANTOM_BOOSTER.get()) return ADVANCED;
        if (block == ModBlocks.TRANSFINITE_PHANTOM_BOOSTER.get()) return TRANSFINITE;
        if (block == ModBlocks.DIMENSIONAL_PHANTOM_BOOSTER.get()) return DIMENSIONAL;
        return null;
    }
}

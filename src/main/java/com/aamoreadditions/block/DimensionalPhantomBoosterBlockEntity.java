package com.aamoreadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Data-less marker block entity; exists only so the client renderer can be attached to the Dimensional Phantom Booster. */
public class DimensionalPhantomBoosterBlockEntity extends BlockEntity {
    public DimensionalPhantomBoosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DIMENSIONAL_PHANTOM_BOOSTER_BE.get(), pos, state);
    }
}

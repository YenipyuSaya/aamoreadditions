package com.aamoreadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Dimensional Phantom Booster: identical to {@link PhantomBoosterBlock} in behaviour. It only carries a data-less block
 * entity so the client can attach a block entity renderer (enchant glint on the rings, charged-creeper style energy shell).
 */
public class DimensionalPhantomBoosterBlock extends PhantomBoosterBlock implements EntityBlock {
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalPhantomBoosterBlockEntity(pos, state);
    }
}

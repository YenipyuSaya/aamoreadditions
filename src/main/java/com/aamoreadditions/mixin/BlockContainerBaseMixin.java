package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeSlots;
import de.ellpeck.actuallyadditions.mod.blocks.base.BlockContainerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops upgrade items whenever an AA machine block is removed (mining, explosions, replacement). */
@Mixin(value = BlockContainerBase.class, remap = false)
public class BlockContainerBaseMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void aamoreadditions$dropUpgrades(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null && UpgradeSlots.supports(be)) UpgradeSlots.drop(level, be);
            if(!level.isClientSide && be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer farmer)
                com.aamoreadditions.farmer.FarmerRingMenus.drop(level,farmer);
        }
    }
}

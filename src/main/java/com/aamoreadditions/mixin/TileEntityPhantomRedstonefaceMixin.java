package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundTargets;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomRedstoneface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After AA's tick, overwrite redstone caches from the bound block in its stored dimension. */
@Mixin(value = TileEntityPhantomRedstoneface.class, remap = false)
public abstract class TileEntityPhantomRedstonefaceMixin {

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static <T extends BlockEntity> void aama$boundRedstone(Level level, BlockPos pos, BlockState state, T t, CallbackInfo ci) {
        if (!(t instanceof TileEntityPhantomRedstoneface tile) || !tile.isBoundThingInRange()) return;
        var target = BoundTargets.resolve(tile);
        if (target.isEmpty()) return;
        BlockState boundState = target.get().state();
        Level targetLevel = target.get().level();
        BlockPos targetPos = target.get().pos();
        for (int i = 0; i < Direction.values().length; i++) {
            Direction facing = Direction.values()[i];
            tile.providesWeak[i] = boundState.getSignal(targetLevel, targetPos, facing);
            tile.providesStrong[i] = boundState.getDirectSignal(targetLevel, targetPos, facing);
        }
    }
}

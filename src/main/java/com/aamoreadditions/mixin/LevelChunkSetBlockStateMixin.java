package com.aamoreadditions.mixin;

import com.aamoreadditions.structure.CasingCrossEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Casing Cross detection: notifies the structure hook after a block state really changed (server side only). */
@Mixin(LevelChunk.class)
public abstract class LevelChunkSetBlockStateMixin {
    @Shadow @Final Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void aamoreadditions$casingCross(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        BlockState old = cir.getReturnValue();
        if (old == null || this.level.isClientSide) return; // null = nothing changed
        CasingCrossEvents.onBlockChanged(this.level, pos, old, state);
    }
}

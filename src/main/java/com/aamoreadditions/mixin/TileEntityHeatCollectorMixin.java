package com.aamoreadditions.mixin;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.effect.UpgradeEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityHeatCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Heat collector:
 *  - ENERGY_PRODUCE (compile-time constant 40, inlined twice in serverTick: the free-space check and
 *    receiveEnergyInternal) -> max(1, round(40 * S));
 *  - the random lava/magma removal (level.setBlockAndUpdate(..., AIR) after disappearTime >= 1000) is skipped
 *    while an upgrade with prevents_lava_consumption is installed.
 * require = 0 + startup constant check: if AA changes, the effect is skipped with a warning instead of crashing.
 */
@Mixin(value = TileEntityHeatCollector.class, remap = false)
public abstract class TileEntityHeatCollectorMixin implements com.aamoreadditions.upgrade.UpgradeResetState {
    @org.spongepowered.asm.mixin.Shadow private int disappearTime;
    @Override public void aamoreadditions$resetUpgradeWork() { disappearTime = 0; }
    @Unique private static final int EXPECTED_ENERGY_PRODUCE = 40;
    private static final String TICK = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V";

    static {
        try {
            int v = TileEntityHeatCollector.class.getField("ENERGY_PRODUCE").getInt(null);
            if (v != EXPECTED_ENERGY_PRODUCE) {
                AAMoreAdditions.LOGGER.warn("aamoreadditions: AA heat collector ENERGY_PRODUCE changed to {}; speed upgrade effect will not apply.", v);
            }
        } catch (ReflectiveOperationException e) {
            AAMoreAdditions.LOGGER.warn("aamoreadditions: could not verify AA heat collector ENERGY_PRODUCE", e);
        }
    }

    @ModifyExpressionValue(method = TICK, require = 0, at = @At(value = "CONSTANT", args = "intValue=40"))
    private static int aamoreadditions$produce(int original, @Local(argsOnly = true) BlockEntity be) {
        return UpgradeEffects.generatorOutput(be, com.aamoreadditions.effect.UpgradeConfig.heatFePerTick(original));
    }

    @WrapOperation(method = TICK, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean aamoreadditions$keepLava(Level level, BlockPos target, BlockState newState, Operation<Boolean> op,
                                            @Local(argsOnly = true) BlockEntity be) {
        if (UpgradeEffects.preventsLavaConsumption(be)) return false;
        return op.call(level, target, newState);
    }
}

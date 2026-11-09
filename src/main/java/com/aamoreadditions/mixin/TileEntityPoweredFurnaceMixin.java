package com.aamoreadditions.mixin;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.effect.UpgradeEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Double Furnace (powered_furnace). AA's SMELT_TIME (80) and ENERGY_USE (25) are compile-time constants inlined
 * into AA's bytecode, so the inlined constants are modified where they are used (AA's values stay untouched):
 *  - static serverTick: "energy >= ENERGY_USE" checks + extractEnergyInternal(ENERGY_USE) -> modified per-tick use;
 *    "smeltTime >= SMELT_TIME" -> modified max time;
 *  - getFirst/SecondTimeToScale: SMELT_TIME -> modified max time (progress bar).
 * require = 0 + a startup check: if a future AA version changes these values, the effect is disabled with a
 * warning instead of crashing (update EXPECTED_* and the args below).
 */
@Mixin(value = TileEntityPoweredFurnace.class, remap = false)
public abstract class TileEntityPoweredFurnaceMixin implements com.aamoreadditions.upgrade.UpgradeResetState {
    @org.spongepowered.asm.mixin.Shadow private boolean lastSmelted;
    @Override public void aamoreadditions$resetUpgradeWork() { lastSmelted = false; }
    @Unique private static final int EXPECTED_SMELT_TIME = 80, EXPECTED_ENERGY_USE = 25;

    static {
        try {
            var f = TileEntityPoweredFurnace.class.getDeclaredField("SMELT_TIME");
            f.setAccessible(true);
            int smelt = f.getInt(null);
            int energy = TileEntityPoweredFurnace.class.getField("ENERGY_USE").getInt(null);
            if (smelt != EXPECTED_SMELT_TIME || energy != EXPECTED_ENERGY_USE) {
                AAMoreAdditions.LOGGER.warn("aamoreadditions: AA powered furnace constants changed (SMELT_TIME={}, ENERGY_USE={}); upgrade effects for it may not apply.", smelt, energy);
            }
        } catch (ReflectiveOperationException e) {
            AAMoreAdditions.LOGGER.warn("aamoreadditions: could not verify AA powered furnace constants", e);
        }
    }

    @ModifyExpressionValue(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At(value = "CONSTANT", args = "intValue=25"), require = 0)
    private static int aamoreadditions$energyUse(int original, @Local(argsOnly = true) BlockEntity be) {
        return UpgradeEffects.energyPerTick(be, original);
    }

    @ModifyExpressionValue(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At(value = "CONSTANT", args = "intValue=80"), require = 0)
    private static int aamoreadditions$smeltTime(int original, @Local(argsOnly = true) BlockEntity be) {
        return UpgradeEffects.time(be, original);
    }

    @ModifyExpressionValue(method = {"getFirstTimeToScale", "getSecondTimeToScale"}, at = @At(value = "CONSTANT", args = "intValue=80"), require = 0)
    private int aamoreadditions$scaleTime(int original) {
        return UpgradeEffects.time((BlockEntity) (Object) this, original);
    }
}

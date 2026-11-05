package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.UpgradeConfig;
import com.aamoreadditions.effect.UpgradeEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityLeafGenerator;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Leaf generator (values read from AA's CommonConfig at runtime, AA untouched):
 *  - LEAF_GENERATOR_COOLDOWN read -> max(1, round(cooldown * cooldown_multiplier)) (speed line);
 *    while idle-sleeping it reads as Integer.MAX_VALUE, so AA just counts up and does not scan;
 *  - LEAF_GENERATOR_CF_PER_LEAF read -> max(1, round(FE * energy_multiplier)) (efficiency line). AA stores it
 *    in a local, so the free-space check and receiveEnergyInternal both use the upgraded value;
 *  - smart sleep: when the scan finds no leaves (breakPositions.isEmpty() == true), sleep for
 *    leaf_generator_idle_sleep_ticks (global config, 0 = off). Not persisted (a reload simply rescans).
 * require = 0: if AA's code shape changes, the effects are skipped instead of crashing.
 */
@Mixin(value = TileEntityLeafGenerator.class, remap = false)
public abstract class TileEntityLeafGeneratorMixin {
    private static final String TICK = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V";
    private static final String CFG = "Lde/ellpeck/actuallyadditions/mod/config/CommonConfig$Machines;";

    @Unique private int aamoreadditions$sleepTicks;

    @ModifyExpressionValue(method = TICK, require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = CFG + "LEAF_GENERATOR_COOLDOWN:Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;", opcode = Opcodes.GETSTATIC)))
    private static int aamoreadditions$cooldown(int original, @Local(argsOnly = true) BlockEntity be) {
        if (be instanceof TileEntityLeafGenerator) {
            TileEntityLeafGeneratorMixin self = (TileEntityLeafGeneratorMixin) (Object) be;
            if (self.aamoreadditions$sleepTicks > 0) {
                self.aamoreadditions$sleepTicks--;
                return Integer.MAX_VALUE; // no scan this tick
            }
        }
        return UpgradeEffects.cooldown(be, original);
    }

    @ModifyExpressionValue(method = TICK, require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = CFG + "LEAF_GENERATOR_CF_PER_LEAF:Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;", opcode = Opcodes.GETSTATIC)))
    private static int aamoreadditions$energyPerLeaf(int original, @Local(argsOnly = true) BlockEntity be) {
        return original <= 0 ? original : UpgradeEffects.leafEnergy(be, com.aamoreadditions.effect.UpgradeConfig.leafFePerLeaf(original));
    }

    @ModifyExpressionValue(method = TICK, require = 0, at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private static boolean aamoreadditions$sleepWhenEmpty(boolean empty, @Local(argsOnly = true) BlockEntity be) {
        if (empty && be instanceof TileEntityLeafGenerator) {
            ((TileEntityLeafGeneratorMixin) (Object) be).aamoreadditions$sleepTicks = UpgradeConfig.leafIdleSleepTicks();
        }
        return empty;
    }
}

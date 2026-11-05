package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.UpgradeEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.ellpeck.actuallyadditions.mod.tile.CustomEnergyStorage;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCoalGenerator;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coal generator speed upgrades, on top of AA's runtime values:
 *  - FE produced per tick (argument of receiveEnergyInternal) -> max(1, round(original * S));
 *  - when a new fuel item starts, the value written to maxBurnTime -> max(1, round(original / S * U))
 *    (currentBurnTime is then copied from it by AA, so the flame stays consistent; a fuel already burning is not changed).
 * require = 0: if AA's code shape changes the effect is skipped (AAMoreAdditions logs a warning on startup checks).
 */
@Mixin(value = TileEntityCoalGenerator.class, remap = false)
public abstract class TileEntityCoalGeneratorMixin {
    private static final String TICK = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V";

    @WrapOperation(method = TICK, require = 0, at = @At(value = "INVOKE",
            target = "Lde/ellpeck/actuallyadditions/mod/tile/CustomEnergyStorage;receiveEnergyInternal(IZ)I"))
    private static int aamoreadditions$produce(CustomEnergyStorage storage, int amount, boolean simulate, Operation<Integer> op,
                                       @Local(argsOnly = true) BlockEntity be) {
        return op.call(storage, UpgradeEffects.generatorOutput(be, com.aamoreadditions.effect.UpgradeConfig.coalFePerTick(amount)), simulate);
    }

    @WrapOperation(method = TICK, require = 0, at = @At(value = "FIELD",
            target = "Lde/ellpeck/actuallyadditions/mod/tile/TileEntityCoalGenerator;maxBurnTime:I", opcode = Opcodes.PUTFIELD))
    private static void aamoreadditions$burnTime(TileEntityCoalGenerator tile, int value, Operation<Void> op) {
        op.call(tile, UpgradeEffects.generatorBurnTime(tile, value));
    }
}
